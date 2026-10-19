package com.tomochi.global.error;

import org.springframework.http.HttpStatus;

/**
 * 특정 도메인에 속하지 않는 오류 코드.
 */
public enum CommonErrorCode implements ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않아요"),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않아요"),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없어요"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요"),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많아요. 잠시 뒤에 다시 시도해 주세요"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 문제가 생겼어요. 잠시 뒤에 다시 시도해 주세요");

	private final HttpStatus status;
	private final String message;

	CommonErrorCode(HttpStatus status, String message) {
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
