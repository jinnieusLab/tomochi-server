package com.tomochi.auth.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료됐어요"),
	TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않아요"),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근할 수 없어요"),
	INVALID_CHALLENGE(HttpStatus.BAD_REQUEST, "로그인 요청이 올바르지 않아요"),
	/** 코드가 없거나, 만료됐거나, 이미 썼거나, verifier가 맞지 않을 때. 어느 쪽인지는 알려주지 않아요. */
	AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "로그인을 다시 시도해 주세요"),
	/** 없거나 만료된 Refresh Token. 앱은 다시 로그인해요. */
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "로그인이 만료됐어요. 다시 로그인해 주세요"),
	/** 같은 토큰으로 보낸 다른 요청이 방금 회전에 성공했어요. 앱은 그 요청이 받은 새 토큰을 써요. */
	REFRESH_TOKEN_ROTATED(HttpStatus.CONFLICT, "이미 새 토큰을 받았어요"),
	/** 폐기된 토큰이 다시 왔어요. 탈취로 보고 그 기기의 토큰을 모두 끊었어요. */
	REFRESH_TOKEN_REUSED(HttpStatus.FORBIDDEN, "보안을 위해 로그아웃했어요. 다시 로그인해 주세요"),
	SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "로그인한 기기를 찾을 수 없어요");

	private final HttpStatus status;
	private final String message;

	AuthErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}
}
