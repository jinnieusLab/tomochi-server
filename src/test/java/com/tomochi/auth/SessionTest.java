package com.tomochi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class SessionTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired JwtDecoder jwtDecoder;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private TokenResponse macbook;
	private TokenResponse imac;

	@BeforeEach
	void setUp() {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		macbook = tokenService.issueForNewLogin(userId, "맥북");
		imac = tokenService.issueForNewLogin(userId, "아이맥");
	}

	@Test
	void Access_Token에_세션_id가_들어_있다() {
		String sid = jwtDecoder.decode(macbook.accessToken()).getClaimAsString("sid");

		assertThat(sid).isNotBlank().isNotEqualTo(jwtDecoder.decode(imac.accessToken()).getClaimAsString("sid"));
	}

	@Test
	void 로그인한_기기_목록을_보여주고_지금_기기를_표시한다() throws Exception {
		mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", bearer(macbook)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.deviceName == '맥북')].current").value(true))
				.andExpect(jsonPath("$[?(@.deviceName == '아이맥')].current").value(false))
				.andExpect(jsonPath("$[0].signedInAt").isNotEmpty())
				.andExpect(jsonPath("$[0].lastUsedAt").isNotEmpty());
	}

	@Test
	void 재발급해도_같은_세션이고_로그인_시각은_그대로다() throws Exception {
		String sid = jwtDecoder.decode(macbook.accessToken()).getClaimAsString("sid");
		TokenResponse rotated = refreshOk(macbook.refreshToken());

		assertThat(jwtDecoder.decode(rotated.accessToken()).getClaimAsString("sid")).isEqualTo(sid);
		mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", bearer(rotated)))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.id == '" + sid + "')].current").value(true));
	}

	@Test
	void 로그아웃하면_그_기기만_재발급이_막힌다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(macbook)))
				.andExpect(status().isNoContent());

		refresh(macbook.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
		refreshOk(imac.refreshToken());
	}

	@Test
	void 로그아웃은_여러_번_해도_된다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(macbook)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(macbook)))
				.andExpect(status().isNoContent());
	}

	@Test
	void 다른_기기를_로그아웃시킨다() throws Exception {
		String imacSession = jwtDecoder.decode(imac.accessToken()).getClaimAsString("sid");

		mockMvc.perform(delete("/api/v1/auth/sessions/{id}", imacSession).header("Authorization", bearer(macbook)))
				.andExpect(status().isNoContent());

		refresh(imac.refreshToken()).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", bearer(macbook)))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].deviceName").value("맥북"));
	}

	@Test
	void 남의_세션은_끊을_수_없다() throws Exception {
		Long otherUser = oauthLoginFacade.login(OAuthProvider.GOOGLE, UUID.randomUUID().toString(), PetKind.CHOCO);
		TokenResponse other = tokenService.issueForNewLogin(otherUser, "남의 맥");
		String otherSession = jwtDecoder.decode(other.accessToken()).getClaimAsString("sid");

		mockMvc.perform(delete("/api/v1/auth/sessions/{id}", otherSession).header("Authorization", bearer(macbook)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));

		refreshOk(other.refreshToken());
	}

	@Test
	void 로그인하지_않으면_세션_API를_쓸_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/auth/sessions"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(TokenResponse tokens) {
		return "Bearer " + tokens.accessToken();
	}

	private TokenResponse refreshOk(String refreshToken) throws Exception {
		String body = refresh(refreshToken).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return jsonMapper.readValue(body, TokenResponse.class);
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
	}
}
