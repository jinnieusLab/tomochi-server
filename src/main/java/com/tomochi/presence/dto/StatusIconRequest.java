package com.tomochi.presence.dto;

import com.tomochi.presence.domain.StatusIcon;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatusIconRequest(
		@Schema(description = "STUDY · CODE · WRITE · DESIGN · MUSIC · FOCUS, 지우려면 null", example = "CODE", nullable = true)
		StatusIcon icon
) {
}
