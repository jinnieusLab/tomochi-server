package com.tomochi.squish.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SquishErrorCode implements ErrorCode {

	/** 같은 친구에게는 10초에 한 번. retryAfterSeconds가 같이 와요. */
	SQUISH_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "같은 친구에게는 10초 뒤에 다시 보낼 수 있어요");

	private final HttpStatus status;
	private final String message;

	SquishErrorCode(HttpStatus status, String message) {
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
