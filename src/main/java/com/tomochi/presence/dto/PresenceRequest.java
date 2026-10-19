package com.tomochi.presence.dto;

import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PresenceRequest(
		@Schema(description = "지금 상태. OFFLINE은 보내지 않아요", example = "WORKING",
				allowableValues = {"WORKING", "RESTING", "AWAY"})
		@NotNull PresenceStatus status,

		@Schema(description = "고른 상태 아이콘. 없으면 null. 작업 중일 때만 친구에게 보여요", example = "STUDY", nullable = true)
		StatusIcon icon
) {
}
