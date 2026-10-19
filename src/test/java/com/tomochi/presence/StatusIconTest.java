package com.tomochi.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.presence.service.PresenceStore;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class StatusIconTest {

	@Autowired MockMvc mockMvc;
	@Autowired PresenceStore presenceStore;
	@Autowired StringRedisTemplate redisTemplate;
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
	void 아이콘을_저장하고_내_정보에_보인다() throws Exception {
		change("{\"icon\": \"CODE\"}").andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(jsonPath("$.statusIcon").value("CODE"));
	}

	@Test
	void 접속_중인_기기의_상태에도_바로_반영하고_TTL은_남긴다() throws Exception {
		presenceStore.update(userId, "mac", PresenceStatus.WORKING, StatusIcon.STUDY, Instant.now());

		change("{\"icon\": \"MUSIC\"}").andExpect(status().isNoContent());

		assertThat(presenceStore.find(userId, Instant.now()).icon()).isEqualTo(StatusIcon.MUSIC);
		assertThat(presenceStore.find(userId, Instant.now()).status()).isEqualTo(PresenceStatus.WORKING);
		List<Long> ttl = redisTemplate.execute((RedisCallback<List<Long>>) connection ->
				connection.hashCommands().hTtl(("presence:" + userId).getBytes(), "mac".getBytes()));
		assertThat(ttl.getFirst()).isBetween(1L, 90L);
	}

	@Test
	void null이면_아이콘을_지운다() throws Exception {
		change("{\"icon\": \"CODE\"}").andExpect(status().isNoContent());
		change("{\"icon\": null}").andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(jsonPath("$.statusIcon").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void 모르는_아이콘은_400() throws Exception {
		change("{\"icon\": \"GAME\"}").andExpect(status().isBadRequest());
	}

	@Test
	void 접속_중이_아니어도_저장한다() throws Exception {
		change("{\"icon\": \"FOCUS\"}").andExpect(status().isNoContent());

		assertThat(presenceStore.find(userId, Instant.now()).status()).isEqualTo(PresenceStatus.OFFLINE);
	}

	private ResultActions change(String body) throws Exception {
		return mockMvc.perform(put("/api/v1/me/status-icon")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}
