package com.tomochi.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 모든 API의 오류 응답 본문. 값이 없는 필드는 응답에서 빠져요.
 *
 * <pre>
 * { "code": "SQUISH_COOLDOWN", "message": "...", "retryAfterSeconds": 7 }
 * { "code": "INVALID_INPUT", "message": "...", "fieldErrors": [{ "field": "nickname", "reason": "..." }] }
 * </pre>
 */
@Schema(description = "오류 응답. 앱은 `code`로 분기하고 `message`는 그대로 보여줘도 돼요")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		@Schema(description = "오류 코드", example = "INVALID_INPUT")
		String code,

		@Schema(description = "사용자에게 보여줄 수 있는 문장", example = "입력값이 올바르지 않아요")
		String message,

		@Schema(description = "다시 시도할 수 있을 때까지 남은 초. 429일 때만 있어요", example = "7", nullable = true)
		Long retryAfterSeconds,

		@Schema(description = "입력 검증에 실패한 필드. 검증 오류일 때만 있어요", nullable = true)
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

	public record FieldError(
			@Schema(description = "필드 이름", example = "nickname") String field,
			@Schema(description = "틀린 이유", example = "공백일 수 없습니다") String reason
	) {
	}
}
