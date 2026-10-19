package com.tomochi.global.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"tomochi.rate-limit.auth-per-ip=3",
		"tomochi.rate-limit.upload-per-user=2",
		"tomochi.rate-limit.default-per-user=4"
})
class RateLimitTest {

	private static final String CHALLENGE = "uVzdJH4DNvZMUVLwHRcytsh0mwMr9gBB_R7DxseoPrw";

	@Autowired MockMvc mockMvc;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	@Test
	void 토큰_교환은_IP당_한도를_넘으면_429와_Retry_After() throws Exception {
		RequestPostProcessor ip = randomIp();
		for (int i = 0; i < 3; i++) {
			tokenRequest(ip).andExpect(status().isBadRequest());
		}

		tokenRequest(ip)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", Matchers.matchesPattern("[1-9][0-9]?")))
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
				.andExpect(jsonPath("$.retryAfterSeconds").value(Matchers.allOf(
						Matchers.greaterThanOrEqualTo(1), Matchers.lessThanOrEqualTo(60))));
		// 다른 IP는 따로 세요.
		tokenRequest(randomIp()).andExpect(status().isBadRequest());
	}

	@Test
	void 로그인_시작도_IP당으로_센다() throws Exception {
		RequestPostProcessor ip = randomIp();
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/api/v1/auth/oauth2/github").param("challenge", CHALLENGE).with(ip))
					.andExpect(status().isFound());
		}

		mockMvc.perform(get("/api/v1/auth/oauth2/github").param("challenge", CHALLENGE).with(ip))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void 구간_업로드는_사용자당_한도가_따로다() throws Exception {
		String me = accessTokenOfNewUser();
		String other = accessTokenOfNewUser();
		for (int i = 0; i < 2; i++) {
			upload(me).andExpect(status().isOk());
		}

		upload(me).andExpect(status().isTooManyRequests());
		upload(other).andExpect(status().isOk());
		// 업로드 한도와 그 밖의 API 한도는 따로예요.
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + me)).andExpect(status().isOk());
	}

	@Test
	void 그_밖의_API는_사용자당_한도를_넘으면_429() throws Exception {
		String me = accessTokenOfNewUser();
		for (int i = 0; i < 4; i++) {
			mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + me)).andExpect(status().isOk());
		}

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + me))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

	@Test
	void Swagger_같은_API_밖_경로는_세지_않는다() throws Exception {
		RequestPostProcessor ip = randomIp();
		for (int i = 0; i < 6; i++) {
			mockMvc.perform(get("/v3/api-docs").with(ip)).andExpect(status().isOk());
		}
	}

	private org.springframework.test.web.servlet.ResultActions tokenRequest(RequestPostProcessor ip) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/token").with(ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"no-such-code\",\"codeVerifier\":\"dBjftJeZ4CVP-mA92Q5qwOFxy4n2PDwq9jJlyfpoPDw\"}"));
	}

	private org.springframework.test.web.servlet.ResultActions upload(String accessToken) throws Exception {
		return mockMvc.perform(post("/api/v1/activity/segments")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"segments\": []}"));
	}

	private String accessTokenOfNewUser() {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		return tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private static RequestPostProcessor randomIp() {
		String ip = "10." + ThreadLocalRandom.current().nextInt(256) + "." + ThreadLocalRandom.current().nextInt(256)
				+ "." + ThreadLocalRandom.current().nextInt(1, 255);
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}
}
