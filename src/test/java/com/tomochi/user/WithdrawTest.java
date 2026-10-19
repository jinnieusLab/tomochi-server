package com.tomochi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.repository.ActivitySegmentWriter;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.batch.WithdrawnUserPurgeJob;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class WithdrawTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired ActivitySegmentWriter writer;
	@Autowired WithdrawnUserPurgeJob purgeJob;

	private String providerUserId;
	private Long userId;
	private TokenResponse tokens;

	@BeforeEach
	void setUp() {
		providerUserId = UUID.randomUUID().toString();
		userId = oauthLoginFacade.login(OAuthProvider.GITHUB, providerUserId, PetKind.HOBBANG);
		tokens = tokenService.issueForNewLogin(userId, "맥북");
		Instant start = Instant.now().minusSeconds(3600);
		writer.upsert(userId, Map.of(UUID.randomUUID().toString(), new TimeRange(start, start.plusSeconds(600))),
				Instant.now());
	}

	@Test
	void 탈퇴하면_바로_로그아웃되고_내_정보를_볼_수_없다() throws Exception {
		withdraw(tokens.accessToken()).andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\": \"" + tokens.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokens.accessToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
		withdraw(tokens.accessToken()).andExpect(status().isNotFound());
	}

	@Test
	void 일주일_안에_다시_로그인하면_되살린다() throws Exception {
		withdraw(tokens.accessToken()).andExpect(status().isNoContent());

		Long again = oauthLoginFacade.login(OAuthProvider.GITHUB, providerUserId, PetKind.CHOCO);
		String accessToken = tokenService.issueForNewLogin(again, "맥북").accessToken();

		assertThat(again).isEqualTo(userId);
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.petKind").value("HOBBANG"));
		assertThat(segmentCount(userId)).isEqualTo(1);
	}

	@Test
	void 일주일이_지나서_로그인하면_지우고_새로_가입한다() throws Exception {
		withdraw(tokens.accessToken()).andExpect(status().isNoContent());
		moveDeletedAtBack(userId, 8);

		Long again = oauthLoginFacade.login(OAuthProvider.GITHUB, providerUserId, PetKind.CHOCO);

		assertThat(again).isNotEqualTo(userId);
		assertThat(userExists(userId)).isFalse();
		assertThat(segmentCount(userId)).isZero();
	}

	@Test
	void 정리_배치는_일주일_지난_탈퇴_계정만_모두_지운다() throws Exception {
		Long stillInGrace = oauthLoginFacade.login(OAuthProvider.GOOGLE, UUID.randomUUID().toString(), PetKind.CHOCO);
		withdraw(tokens.accessToken()).andExpect(status().isNoContent());
		withdraw(tokenService.issueForNewLogin(stillInGrace, "맥").accessToken()).andExpect(status().isNoContent());
		moveDeletedAtBack(userId, 8);

		purgeJob.run();

		assertThat(userExists(userId)).isFalse();
		assertThat(segmentCount(userId)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_token WHERE user_id = ?",
				Integer.class, userId)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth_account WHERE user_id = ?",
				Integer.class, userId)).isZero();
		assertThat(userExists(stillInGrace)).isTrue();
	}

	private org.springframework.test.web.servlet.ResultActions withdraw(String accessToken) throws Exception {
		return mockMvc.perform(delete("/api/v1/me").header("Authorization", "Bearer " + accessToken));
	}

	private void moveDeletedAtBack(Long id, int days) {
		jdbcTemplate.update("UPDATE users SET deleted_at = deleted_at - INTERVAL ? DAY WHERE id = ?", days, id);
	}

	private boolean userExists(Long id) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id) == 1;
	}

	private Integer segmentCount(Long id) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activity_segment WHERE user_id = ?", Integer.class, id);
	}
}
