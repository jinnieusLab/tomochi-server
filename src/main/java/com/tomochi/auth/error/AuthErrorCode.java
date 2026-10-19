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
	AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "로그인을 다시 시도해 주세요");

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
