package com.tomochi.activity.controller;

import com.tomochi.activity.dto.DayResponse;
import com.tomochi.activity.dto.DaysResponse;
import com.tomochi.activity.error.ActivityErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;

@Tag(name = "활동 기록")
public interface DailyActivityApi {

	@Operation(summary = "하루 기록", description = """
			내 프로필의 하루 상자에 쓸 그날 함께한 시간과 30분 칸 48개예요. 날짜는 **내 시간대** 기준이에요.
			오늘은 지금까지의 값이고, 기기가 여러 대여도 겹친 시간은 한 번만 세요.
			오늘보다 뒤이거나 6개월(186일)보다 전이면 404예요.""")
	@ApiResponse(responseCode = "200", description = "그날 기록 (없으면 0)")
	@ApiErrorCodes(value = ActivityErrorCode.class, codes = "DAY_OUT_OF_RANGE")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	DayResponse findDay(@CurrentUser AuthUser user,
			@Parameter(description = "yyyy-MM-dd", example = "2026-10-16") LocalDate date);

	@Operation(summary = "여러 날 기록", description = """
			‹ ›로 넘길 때 미리 받아 둘 수 있게 여러 날을 한 번에 줘요. `from`부터 `to`까지(둘 다 포함) 최대 31일이에요.
			기록이 없는 날도 0으로 들어 있어요.""")
	@ApiResponse(responseCode = "200", description = "날짜 순 기록")
	@ApiErrorCodes(value = ActivityErrorCode.class, codes = {"INVALID_DATE_RANGE", "DAY_OUT_OF_RANGE"})
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	DaysResponse findDays(@CurrentUser AuthUser user,
			@Parameter(description = "시작일 yyyy-MM-dd", example = "2026-10-11") LocalDate from,
			@Parameter(description = "끝일 yyyy-MM-dd (포함)", example = "2026-10-17") LocalDate to);
}
