package com.tomochi.global.error;

import lombok.Getter;

/**
 * 서비스 로직에서 정해진 오류를 낼 때 던지는 예외. 전역 예외 처리에서 오류 응답으로 바꿔요.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	/** 다시 시도할 수 있을 때까지 남은 초. 429 응답의 Retry-After 헤더에도 써요. */
	private final Long retryAfterSeconds;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, null);
	}

	public BusinessException(ErrorCode errorCode, Long retryAfterSeconds) {
		super(errorCode.message());
		this.errorCode = errorCode;
		this.retryAfterSeconds = retryAfterSeconds;
	}
}
