package com.tomochi.presence.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PresenceErrorCode implements ErrorCode {

	/** OFFLINE은 서버가 정해요. 앱은 WORKING·RESTING·AWAY만 보내요. */
	INVALID_PRESENCE_STATUS(HttpStatus.BAD_REQUEST, "보낼 수 없는 상태예요");

	private final HttpStatus status;
	private final String message;

	PresenceErrorCode(HttpStatus status, String message) {
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
