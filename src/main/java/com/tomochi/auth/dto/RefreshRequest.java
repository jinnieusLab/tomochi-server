package com.tomochi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
		@Schema(description = "가장 최근에 받은 Refresh Token", example = "Yv2mQx8Kp0rT4sW7uZ1cB3dF6gH9jL2nP5qS8vX0yA4")
		@NotBlank @Size(max = 128) String refreshToken
) {
}
