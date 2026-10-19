package com.tomochi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class RefreshTokenRotationTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired JwtDecoder jwtDecoder;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long userId;
	private TokenResponse login;

	@BeforeEach
	void setUp() {
		userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		login = tokenService.issueForNewLogin(userId, "내 맥북");
	}

	@Test
	void 재발급하면_새_토큰_한_쌍을_주고_쓴_토큰은_폐기한다() throws Exception {
		TokenResponse rotated = refreshOk(login.refreshToken());

		assertThat(rotated.refreshToken()).isNotEqualTo(login.refreshToken());
		assertThat(jwtDecoder.decode(rotated.accessToken()).getSubject()).isEqualTo(String.valueOf(userId));
		assertThat(revokedAt(login.refreshToken())).isNotNull();
		assertThat(revokedAt(rotated.refreshToken())).isNull();
		assertThat(familyOf(rotated.refreshToken())).isEqualTo(familyOf(login.refreshToken()));
	}

	@Test
	void 회전을_이어서_할_수_있다() throws Exception {
		TokenResponse second = refreshOk(login.refreshToken());
		TokenResponse third = refreshOk(second.refreshToken());

		assertThat(activeCountInFamily(familyOf(login.refreshToken()))).isEqualTo(1);
		assertThat(revokedAt(third.refreshToken())).isNull();
	}

	@Test
	void 방금_회전된_토큰이_다시_오면_동시_요청으로_보고_family는_그대로_둔다() throws Exception {
		TokenResponse rotated = refreshOk(login.refreshToken());

		refresh(login.refreshToken())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_ROTATED"));

		// 이긴 요청이 받은 새 토큰은 계속 쓸 수 있어요.
		refreshOk(rotated.refreshToken());
	}

	@Test
	void 유예_시간이_지난_뒤_폐기된_토큰이_오면_탈취로_보고_family를_끊는다() throws Exception {
		TokenResponse rotated = refreshOk(login.refreshToken());
		moveRevokedAtBack(login.refreshToken(), 60);

		refresh(login.refreshToken())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

		// 앱이 갖고 있던 새 토큰도 끊겨서, 앱도 다시 로그인해야 해요.
		assertThat(revokedAt(rotated.refreshToken())).isNotNull();
		assertThat(activeCountInFamily(familyOf(login.refreshToken()))).isZero();
		refresh(rotated.refreshToken())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
	}

	@Test
	void 탈취로_끊어도_다른_기기의_family는_그대로다() throws Exception {
		TokenResponse otherDevice = tokenService.issueForNewLogin(userId, "회사 맥");
		refreshOk(login.refreshToken());
		moveRevokedAtBack(login.refreshToken(), 60);

		refresh(login.refreshToken()).andExpect(status().isForbidden());

		refreshOk(otherDevice.refreshToken());
	}

	@Test
	void 없는_토큰은_REFRESH_TOKEN_INVALID() throws Exception {
		refresh("no-such-refresh-token")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
	}

	@Test
	void 만료된_토큰은_REFRESH_TOKEN_INVALID이고_회전하지_않는다() throws Exception {
		jdbcTemplate.update("UPDATE refresh_token SET expires_at = NOW(6) - INTERVAL 1 SECOND WHERE token_hash = ?",
				sha256Hex(login.refreshToken()));

		refresh(login.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
		assertThat(revokedAt(login.refreshToken())).isNull();
	}

	private TokenResponse refreshOk(String refreshToken) throws Exception {
		String body = refresh(refreshToken)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return jsonMapper.readValue(body, TokenResponse.class);
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
	}

	private Object revokedAt(String refreshToken) throws Exception {
		return jdbcTemplate.queryForObject("SELECT revoked_at FROM refresh_token WHERE token_hash = ?",
				Object.class, sha256Hex(refreshToken));
	}

	private String familyOf(String refreshToken) throws Exception {
		return jdbcTemplate.queryForObject("SELECT family_id FROM refresh_token WHERE token_hash = ?",
				String.class, sha256Hex(refreshToken));
	}

	private Integer activeCountInFamily(String familyId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM refresh_token WHERE family_id = ? AND revoked_at IS NULL", Integer.class, familyId);
	}

	private void moveRevokedAtBack(String refreshToken, int seconds) throws Exception {
		jdbcTemplate.update("UPDATE refresh_token SET revoked_at = revoked_at - INTERVAL ? SECOND WHERE token_hash = ?",
				seconds, sha256Hex(refreshToken));
	}

	static String sha256Hex(String value) throws Exception {
		return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)));
	}
}
