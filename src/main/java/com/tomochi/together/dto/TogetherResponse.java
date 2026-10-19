package com.tomochi.together.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "친구와 함께한 날 잔디. 스트릭(연속 일수)·다른 친구와 비교는 없어요")
public record TogetherResponse(
		@Schema(description = "잔디 시작일 (일요일, 내 시간대)", example = "2026-06-21")
		LocalDate from,

		@Schema(description = "오늘 (내 시간대)", example = "2026-10-17")
		LocalDate to,

		@Schema(description = "겹친 시간이 있는 날만, 날짜 순. 진하기 4단계는 앱이 정해요")
		List<Day> days,

		@Schema(description = "기간 안에 함께한 날 수", example = "55")
		int totalDays
) {

	public record Day(
			@Schema(description = "날짜", example = "2026-10-12") LocalDate date,
			@Schema(description = "그날 둘이 함께한 분 (1초라도 겹치면 1분)", example = "100") long minutes
	) {
	}
}
