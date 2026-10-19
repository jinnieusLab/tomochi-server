package com.tomochi.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record DaysResponse(
		@Schema(description = "from부터 to까지 날짜 순. 기록이 없는 날도 0으로 들어 있어요")
		List<DayResponse> days
) {
}
