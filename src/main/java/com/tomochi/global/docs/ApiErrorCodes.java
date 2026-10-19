package com.tomochi.global.docs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 API가 낼 수 있는 오류 코드. Swagger 응답에 상태 코드별로 예시가 붙어요.
 * 상태 코드와 문구는 {@code ErrorCode} enum에서 읽어서, 문구를 바꿔도 문서가 따로 놀지 않아요.
 *
 * <pre>
 * &#64;ApiErrorCodes(value = AuthErrorCode.class, codes = {"REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_REUSED"})
 * TokenResponse refresh(RefreshRequest request);
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCodes.List.class)
public @interface ApiErrorCodes {

	/** {@code ErrorCode}를 구현한 enum */
	Class<? extends Enum<?>> value();

	/** enum 상수 이름 */
	String[] codes();

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		ApiErrorCodes[] value();
	}
}
