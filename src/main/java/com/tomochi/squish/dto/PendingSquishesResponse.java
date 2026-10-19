package com.tomochi.squish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record PendingSquishesResponse(
		@Schema(description = "친구별로 묶은 아직 안 받은 꾹. 최근에 보낸 친구부터")
		List<Item> items
) {

	public record Item(
			@Schema(description = "보낸 친구 id", example = "34")
			Long fromFriendId,

			@Schema(description = "보낸 꾹 수. \"자리 비운 동안 꾹 5개\" 알림에 써요", example = "5")
			int count,

			@Schema(description = "마지막으로 보낸 시각", example = "2026-10-17T10:03:00Z")
			Instant lastSentAt,

			@Schema(description = "ack할 때 보낼 꾹 id들")
			List<Long> squishIds
	) {
	}
}
