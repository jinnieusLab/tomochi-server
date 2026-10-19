package com.tomochi.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "이번 주(일요일~토요일, 내 시간대) 합계")
public record WeekResponse(
		@Schema(description = "이번 주 일요일", example = "2026-10-11")
		LocalDate weekStart,

		@Schema(description = "이번 주 일요일부터 지금까지 함께한 시간(초)", example = "51600")
		long seconds,

		@Schema(description = "이번 주에 친구 누구와든 겹쳐 작업한 날 수. 둘이 함께한 시간 집계(TGTR-3) 전까지는 0", example = "3")
		int togetherDays
) {
}
