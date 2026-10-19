package com.tomochi.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.service.PresenceStore;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class PresenceApiTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired PresenceStore store;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long userId;
	private String accessToken;

	@BeforeEach
	void setUp() {
		userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	@Test
	void 상태를_받으면_204이고_친구에게_보일_상태가_된다() throws Exception {
		heartbeat("{\"status\": \"WORKING\", \"icon\": \"STUDY\"}").andExpect(status().isNoContent());

		assertThat(store.find(userId, Instant.now()).status()).isEqualTo(PresenceStatus.WORKING);
	}

	@Test
	void OFFLINE은_보낼_수_없다() throws Exception {
		heartbeat("{\"status\": \"OFFLINE\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PRESENCE_STATUS"));
	}

	@Test
	void 모르는_상태나_아이콘은_400() throws Exception {
		heartbeat("{\"status\": \"SLEEPING\"}").andExpect(status().isBadRequest());
		heartbeat("{\"status\": \"WORKING\", \"icon\": \"GAME\"}").andExpect(status().isBadRequest());
		heartbeat("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 마지막_접속_시각은_5분에_한_번만_바꾼다() throws Exception {
		heartbeat("{\"status\": \"WORKING\"}").andExpect(status().isNoContent());
		LocalDateTime first = lastSeen();
		heartbeat("{\"status\": \"WORKING\"}").andExpect(status().isNoContent());

		assertThat(first).isNotNull();
		assertThat(lastSeen()).isEqualTo(first);

		jdbcTemplate.update("UPDATE users SET last_seen_at = last_seen_at - INTERVAL 6 MINUTE WHERE id = ?", userId);
		heartbeat("{\"status\": \"WORKING\"}").andExpect(status().isNoContent());
		assertThat(lastSeen()).isAfter(first.minusMinutes(1));
	}

	private ResultActions heartbeat(String body) throws Exception {
		return mockMvc.perform(put("/api/v1/presence")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private LocalDateTime lastSeen() {
		return jdbcTemplate.queryForObject("SELECT last_seen_at FROM users WHERE id = ?", LocalDateTime.class, userId);
	}
}
