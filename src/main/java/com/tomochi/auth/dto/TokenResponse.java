package com.tomochi.auth.dto;

/**
 * @param expiresIn Access Token이 만료되기까지 남은 초
 */
public record TokenResponse(
		String accessToken,
		String refreshToken,
		long expiresIn
) {
}
