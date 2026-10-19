package com.tomochi.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 모든 API의 오류 응답 본문. 값이 없는 필드는 응답에서 빠져요.
 *
 * <pre>
 * { "code": "SQUISH_COOLDOWN", "message": "...", "retryAfterSeconds": 7 }
 * { "code": "INVALID_INPUT", "message": "...", "fieldErrors": [{ "field": "nickname", "reason": "..." }] }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		String code,
		String message,
		Long retryAfterSeconds,
		List<FieldError> fieldErrors
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.code(), errorCode.message(), null, null);
	}

	public static ErrorResponse of(ErrorCode errorCode, Long retryAfterSeconds) {
		return new ErrorResponse(errorCode.code(), errorCode.message(), retryAfterSeconds, null);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
		return new ErrorResponse(errorCode.code(), errorCode.message(), null, fieldErrors);
	}

	public record FieldError(String field, String reason) {
	}
}
