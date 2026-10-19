package com.tomochi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
		@Schema(description = "Access Token (JWT, 30분). `Authorization: Bearer {accessToken}`로 보내요",
				example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0b21vY2hpIiwic3ViIjoiMSJ9.signature")
		String accessToken,

		@Schema(description = "Refresh Token (30일). 쓸 때마다 새 값으로 바뀌어요. Keychain에 보관해요",
				example = "Yv2mQx8Kp0rT4sW7uZ1cB3dF6gH9jL2nP5qS8vX0yA4")
		String refreshToken,

		@Schema(description = "Access Token이 만료되기까지 남은 초", example = "1800")
		long expiresIn
) {
}
