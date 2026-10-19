package com.tomochi.activity.dto;

import com.tomochi.activity.domain.SegmentPolicy;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UploadSegmentsRequest(
		@ArraySchema(arraySchema = @Schema(description = "올릴 구간들 (최대 200개)"), maxItems = SegmentPolicy.MAX_SEGMENTS_PER_REQUEST)
		@NotNull @Size(max = SegmentPolicy.MAX_SEGMENTS_PER_REQUEST) List<@NotNull @Valid SegmentRequest> segments
) {
}
