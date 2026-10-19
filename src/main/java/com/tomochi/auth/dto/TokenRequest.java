package com.tomochi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param code         로그인 직후 {@code tomochi://auth/callback?code=}로 받은 1회용 코드
 * @param codeVerifier 로그인을 시작할 때 만든 무작위 문자열 (challenge의 원문)
 * @param deviceName   세션 목록에 보일 기기 이름. 없으면 "Mac"
 */
public record TokenRequest(
		@NotBlank String code,
		@NotBlank @Size(min = 43, max = 128) String codeVerifier,
		@Size(max = 100) String deviceName
) {

	private static final String DEFAULT_DEVICE_NAME = "Mac";

	public String deviceNameOrDefault() {
		return deviceName == null || deviceName.isBlank() ? DEFAULT_DEVICE_NAME : deviceName.strip();
	}
}
