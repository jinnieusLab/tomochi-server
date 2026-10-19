package com.tomochi.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class UnfriendTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired UserRepository userRepository;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	@Test
	void 끊으면_양쪽_목록에서_사라지고_숨김_설정도_지운다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		mockMvc.perform(put("/api/v1/friends/{id}/hidden", me).header("Authorization", bearer(minji)));

		mockMvc.perform(delete("/api/v1/friends/{id}", minji).header("Authorization", bearer(me)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/friends").param("includeHidden", "true").header("Authorization", bearer(me)))
				.andExpect(jsonPath("$.friends").isEmpty());
		mockMvc.perform(get("/api/v1/friends").param("includeHidden", "true").header("Authorization", bearer(minji)))
				.andExpect(jsonPath("$.friends").isEmpty())
				.andExpect(jsonPath("$.hiddenCount").value(0));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friend_setting WHERE owner_id IN (?, ?)",
				Integer.class, me, minji)).isZero();
	}

	@Test
	void 끊은_뒤에는_초대_코드로_다시_친구가_될_수_있다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		mockMvc.perform(delete("/api/v1/friends/{id}", minji).header("Authorization", bearer(me)))
				.andExpect(status().isNoContent());

		String code = userRepository.findById(minji).orElseThrow().getInviteCode();
		mockMvc.perform(post("/api/v1/friends").header("Authorization", bearer(me))
						.contentType(MediaType.APPLICATION_JSON).content("{\"inviteCode\": \"" + code + "\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void 친구가_아니면_404() throws Exception {
		Long me = newUser();

		mockMvc.perform(delete("/api/v1/friends/{id}", newUser()).header("Authorization", bearer(me)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND"));
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
