package com.tomochi.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tomochi.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 보호된 경로에 Access Token 없이, 혹은 틀린 토큰으로 오면 401을 오류 형식으로 돌려줘요.
 * 아직 보호된 API가 없어서, 인증을 통과하면 404(없는 주소)가 나는지로 확인해요.
 */
@IntegrationTest
@AutoConfigureMockMvc
class JwtAuthenticationTest {

	private static final String PROTECTED_PATH = "/api/v1/protected-check";

	@Autowired MockMvc mockMvc;
	@Autowired JwtEncoder jwtEncoder;

	@Test
	void 토큰이_없으면_UNAUTHENTICATED() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 올바른_토큰이면_통과한다() throws Exception {
		String token = token(jwtEncoder, "tomochi", Instant.now(), Duration.ofMinutes(30));

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void 만료된_토큰이면_TOKEN_EXPIRED() throws Exception {
		String token = token(jwtEncoder, "tomochi", Instant.now().minus(Duration.ofHours(1)), Duration.ofMinutes(30));

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}

	@Test
	void 다른_키로_서명한_토큰이면_TOKEN_INVALID() throws Exception {
		SecretKey otherKey = new SecretKeySpec(
				"another-secret-key-that-is-long-enough!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
		String token = token(otherEncoder, "tomochi", Instant.now(), Duration.ofMinutes(30));

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
	}

	@Test
	void 다른_키로_서명한_만료_토큰도_TOKEN_EXPIRED가_아니라_TOKEN_INVALID() throws Exception {
		SecretKey otherKey = new SecretKeySpec(
				"another-secret-key-that-is-long-enough!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
		String token = token(otherEncoder, "tomochi", Instant.now().minus(Duration.ofHours(1)), Duration.ofMinutes(30));

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
	}

	@Test
	void 발급자가_다르면_TOKEN_INVALID() throws Exception {
		String token = token(jwtEncoder, "someone-else", Instant.now(), Duration.ofMinutes(30));

		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
	}

	@Test
	void 형식이_깨진_토큰이면_TOKEN_INVALID() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
	}

	private static String token(JwtEncoder encoder, String issuer, Instant issuedAt, Duration ttl) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject("1")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(ttl))
				.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}
}
