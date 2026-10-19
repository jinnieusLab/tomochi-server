package com.tomochi.app.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AppErrorCode implements ErrorCode {

	APP_UPDATE_REQUIRED(HttpStatus.UPGRADE_REQUIRED, "새 버전을 받아 주세요");

	private final HttpStatus status;
	private final String message;

	AppErrorCode(HttpStatus status, String message) {
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
