package com.tomochi.squish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AckSquishesRequest(
		@Schema(description = "받은 꾹 id들 (최대 500개)", example = "[101, 102]")
		@NotNull @Size(max = 500) List<@NotNull Long> squishIds
) {
}
