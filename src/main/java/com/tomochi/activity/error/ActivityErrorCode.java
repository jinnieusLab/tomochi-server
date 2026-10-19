package com.tomochi.activity.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ActivityErrorCode implements ErrorCode {

	/** 오늘보다 뒤이거나, 보관 기간(6개월)보다 전이에요. */
	DAY_OUT_OF_RANGE(HttpStatus.NOT_FOUND, "그날의 기록은 볼 수 없어요"),
	INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "기간은 시작일부터 31일까지 볼 수 있어요");

	private final HttpStatus status;
	private final String message;

	ActivityErrorCode(HttpStatus status, String message) {
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
