package com.tomochi.friend.error;

import com.tomochi.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum FriendErrorCode implements ErrorCode {

	INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 코드를 찾을 수 없어요"),
	OWN_INVITE_CODE(HttpStatus.BAD_REQUEST, "내 초대 코드예요"),
	/** 나나 상대가 이미 친구 20명이에요. */
	FRIEND_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "친구는 20명까지 맺을 수 있어요"),
	FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "친구를 찾을 수 없어요");

	private final HttpStatus status;
	private final String message;

	FriendErrorCode(HttpStatus status, String message) {
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
