package com.tomochi.user.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없어요"),
	INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 1~12자로, 줄바꿈 없이 적어 주세요"),
	INVALID_PET_KIND(HttpStatus.BAD_REQUEST, "없는 펫이에요");

	private final HttpStatus status;
	private final String message;

	UserErrorCode(HttpStatus status, String message) {
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
