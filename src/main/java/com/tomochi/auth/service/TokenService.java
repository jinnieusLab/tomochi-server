package com.tomochi.auth.service;

import com.tomochi.auth.config.AuthProperties;
import com.tomochi.auth.domain.RefreshToken;
import com.tomochi.auth.domain.RefreshTokenRepository;
import com.tomochi.auth.dto.TokenResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access Token(JWT)과 Refresh Token(불투명 문자열)을 발급해요.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

	private static final int REFRESH_TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();
	private final JwtEncoder jwtEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthProperties authProperties;
	private final Clock clock;

	/** 새 로그인. 기기 하나에 Refresh Token family 하나를 새로 열어요. */
	@Transactional
	public TokenResponse issueForNewLogin(Long userId, String deviceName) {
		Instant now = clock.instant();
		String refreshToken = randomToken();
		refreshTokenRepository.save(new RefreshToken(
				userId,
				UUID.randomUUID().toString(),
				hash(refreshToken),
				deviceName,
				now,
				now.plus(authProperties.refreshTokenTtl())));
		return new TokenResponse(accessToken(userId, now), refreshToken, authProperties.accessTokenTtl().toSeconds());
	}

	private String accessToken(Long userId, Instant now) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(authProperties.issuer())
				.subject(String.valueOf(userId))
				.issuedAt(now)
				.expiresAt(now.plus(authProperties.accessTokenTtl()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private String randomToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * Refresh Token은 32바이트 무작위 값이라 사전 공격이 통하지 않아서, 솔트 없는 SHA-256으로 충분해요.
	 * 그 대신 해시로 바로 찾을 수 있어요 ({@code token_hash} 유니크 인덱스).
	 */
	static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
