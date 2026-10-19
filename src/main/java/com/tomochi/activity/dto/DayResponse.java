package com.tomochi.activity.dto;

import com.tomochi.activity.domain.DailyActivity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "하루의 토모찌와 함께한 시간")
public record DayResponse(
		@Schema(description = "날짜 (내 시간대 기준)", example = "2026-10-16")
		LocalDate date,

		@Schema(description = "그날 함께한 시간(초). 기기가 여러 대여도 겹친 시간은 한 번만, 하루 20시간까지", example = "10440")
		long seconds,

		@Schema(description = "0시부터 30분 칸 48개. 그 30분에 1초라도 작업했으면 '1'. 앱은 6시~24시 36칸만 그려요",
				example = "000000000000000000111110001110110000000011100000")
		String slots
) {

	public static DayResponse from(DailyActivity day) {
		return new DayResponse(day.date(), day.seconds(), day.slots());
	}
}
