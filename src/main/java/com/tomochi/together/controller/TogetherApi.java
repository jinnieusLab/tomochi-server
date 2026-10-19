package com.tomochi.together.controller;

import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.together.dto.TogetherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(name = "둘이 함께한 시간", description = "나와 친구의 작업 구간이 겹친 시간. 친구 프로필의 \"민지와 함께한 날\" 잔디예요")
public interface TogetherApi {

	@Operation(summary = "친구와 함께한 날 잔디", description = """
			이번 주 일요일에서 (weeks - 1)주 전 일요일부터 오늘까지예요 (기본 17주, 내 시간대).
			겹친 시간이 있는 날만 와요. 진하기 4단계(없음 / 30분 미만 / 30분~2시간 / 2시간 이상)는 앱이 정해요.
			오늘은 지금까지의 값이고 5분마다 갱신돼요. 스트릭·다른 친구와 비교는 주지 않아요.""")
	@ApiResponse(responseCode = "200", description = "잔디")
	@ApiErrorCodes(value = FriendErrorCode.class, codes = "FRIEND_NOT_FOUND")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	TogetherResponse together(@CurrentUser AuthUser user,
			@Parameter(description = "친구 사용자 id", example = "34") Long friendId,
			@Parameter(description = "몇 주 (1~17)", example = "17") @Min(1) @Max(17) int weeks);
}
