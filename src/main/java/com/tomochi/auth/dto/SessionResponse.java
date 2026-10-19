package com.tomochi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "로그인한 기기 하나")
public record SessionResponse(
		@Schema(description = "세션 id. 다른 기기를 로그아웃할 때 써요", example = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b")
		String id,

		@Schema(description = "로그인할 때 앱이 보낸 기기 이름", example = "지민의 맥북 프로")
		String deviceName,

		@Schema(description = "이 기기에서 로그인한 시각 (UTC)", example = "2026-10-01T09:12:00Z")
		Instant signedInAt,

		@Schema(description = "마지막으로 토큰을 재발급한 시각 (UTC). 앱이 켜져 있으면 30분마다 바뀌어요",
				example = "2026-10-19T13:40:00Z")
		Instant lastUsedAt,

		@Schema(description = "지금 요청을 보낸 기기인지", example = "true")
		boolean current
) {
}
