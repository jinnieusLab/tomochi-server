package com.tomochi.squish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.global.error.BusinessException;
import com.tomochi.squish.error.SquishErrorCode;
import com.tomochi.squish.service.SquishService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class SendSquishTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired SquishService squishService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long me;
	private Long minji;

	@BeforeEach
	void setUp() {
		me = newUser();
		minji = newUser();
		befriend(me, minji);
	}

	@Test
	void 꾹을_보내면_202이고_전달_대기로_남는다() throws Exception {
		send(me, minji).andExpect(status().isAccepted());

		assertThat(pending(me, minji)).isEqualTo(1);
	}

	@Test
	void 같은_친구에게는_10초에_한_번() throws Exception {
		send(me, minji).andExpect(status().isAccepted());

		send(me, minji)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", Matchers.matchesPattern("([1-9]|10)")))
				.andExpect(jsonPath("$.code").value("SQUISH_COOLDOWN"))
				.andExpect(jsonPath("$.retryAfterSeconds").value(Matchers.allOf(
						Matchers.greaterThanOrEqualTo(1), Matchers.lessThanOrEqualTo(10))));
		assertThat(pending(me, minji)).isEqualTo(1);
	}

	@Test
	void 다른_친구에게는_바로_보낼_수_있고_상대도_나에게_보낼_수_있다() throws Exception {
		Long other = newUser();
		befriend(me, other);
		send(me, minji).andExpect(status().isAccepted());

		send(me, other).andExpect(status().isAccepted());
		send(minji, me).andExpect(status().isAccepted());
	}

	@Test
	void 친구가_아니면_404() throws Exception {
		send(me, newUser())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND"));
	}

	@Test
	void 상대가_나를_숨겼으면_성공처럼_답하고_전달하지_않는다() throws Exception {
		mockMvc.perform(put("/api/v1/friends/{id}/hidden", me).header("Authorization", bearer(minji)))
				.andExpect(status().isNoContent());

		send(me, minji).andExpect(status().isAccepted());

		assertThat(pending(me, minji)).isZero();
	}

	@Test
	void 동시에_10번_눌러도_하나만_보낸다() throws Exception {
		int threads = 10;
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
			for (int i = 0; i < threads; i++) {
				results.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					try {
						squishService.send(me, minji);
						return true;
					} catch (BusinessException e) {
						assertThat(e.getErrorCode()).isEqualTo(SquishErrorCode.SQUISH_COOLDOWN);
						return false;
					}
				}));
			}
			ready.await();
			start.countDown();
			int sent = 0;
			for (Future<Boolean> result : results) {
				sent += result.get() ? 1 : 0;
			}
			assertThat(sent).isEqualTo(1);
		}
		assertThat(pending(me, minji)).isEqualTo(1);
	}

	@Test
	void 친구를_끊으면_안_받은_꾹도_지운다() throws Exception {
		send(me, minji).andExpect(status().isAccepted());

		mockMvc.perform(delete("/api/v1/friends/{id}", minji).header("Authorization", bearer(me)))
				.andExpect(status().isNoContent());

		assertThat(pending(me, minji)).isZero();
	}

	private ResultActions send(Long from, Long to) throws Exception {
		return mockMvc.perform(post("/api/v1/friends/{id}/squishes", to).header("Authorization", bearer(from)));
	}

	private Integer pending(Long from, Long to) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM squish WHERE from_user_id = ? AND to_user_id = ? AND delivered_at IS NULL",
				Integer.class, from, to);
	}

	private String bearer(Long userId) {
		return "Bearer " + tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}
}
