package com.tomochi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(
		@Schema(description = "로그인 직후 `tomochi://auth/callback?code=`로 받은 1회용 코드 (60초, 한 번만)",
				example = "q3Jd0f1kz9X0vN2bS7tYwE5uR8mA4cL6hG1pK3oI2jU")
		@NotBlank String code,

		@Schema(description = "로그인을 시작할 때 만든 무작위 문자열(challenge의 원문). 43~128자",
				example = "dBjftJeZ4CVP-mA92Q5qwOFxy4n2PDwq9jJlyfpoPDw")
		@NotBlank @Size(min = 43, max = 128) String codeVerifier,

		@Schema(description = "세션 목록에 보일 기기 이름. 없으면 \"Mac\"", example = "지민의 맥북 프로", nullable = true)
		@Size(max = 100) String deviceName
) {

	private static final String DEFAULT_DEVICE_NAME = "Mac";

	public String deviceNameOrDefault() {
		return deviceName == null || deviceName.isBlank() ? DEFAULT_DEVICE_NAME : deviceName.strip();
	}
}
