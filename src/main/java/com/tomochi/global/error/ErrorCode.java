package com.tomochi.global.error;

import org.springframework.http.HttpStatus;

/**
 * 오류 코드. 도메인마다 이 인터페이스를 구현한 enum을 둬요 (예: CommonErrorCode, AuthErrorCode).
 */
public interface ErrorCode {

	HttpStatus status();

	/** 앱이 분기에 쓰는 값. enum 이름을 그대로 써요 (예: SQUISH_COOLDOWN). */
	String code();

	/** 사용자에게 보여줄 수 있는 문장. */
	String message();
}
