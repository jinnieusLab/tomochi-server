package com.tomochi.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Access Token(JWT, HS256). 서버가 발급과 검증을 모두 해서 비밀 키 하나로 충분해요.
 */
@Configuration
public class JwtConfig {

	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	public SecretKey jwtSecretKey(AuthProperties authProperties) {
		if (authProperties.jwtSecret() == null || authProperties.jwtSecret().isBlank()) {
			throw new IllegalStateException("JWT_SECRET 환경 변수가 필요해요 (openssl rand -base64 48)");
		}
		byte[] secret = Base64.getDecoder().decode(authProperties.jwtSecret());
		if (secret.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET은 32바이트 이상이어야 해요");
		}
		return new SecretKeySpec(secret, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	/** 서명, 만료(exp), 발급자(iss)를 확인해요. */
	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, AuthProperties authProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(authProperties.issuer()));
		return decoder;
	}
}
