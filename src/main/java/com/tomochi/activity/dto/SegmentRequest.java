package com.tomochi.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "작업 구간 하나 [start, end)")
public record SegmentRequest(
		@Schema(description = "앱이 구간마다 만든 UUID. 같은 구간을 늘려 다시 보낼 때도 같은 값을 써요",
				example = "8f6c2a1e-4b7d-4c3a-9f10-2d5e6a7b8c9d")
		String clientSegmentId,

		@Schema(description = "작업 중이 시작된 시각 (UTC)", example = "2026-10-17T09:02:11Z")
		Instant start,

		@Schema(description = "마지막 입력 시각 (UTC). 열린 구간이면 지금까지", example = "2026-10-17T10:47:40Z")
		Instant end
) {
}
