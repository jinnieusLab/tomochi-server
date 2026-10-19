package com.tomochi.activity.dto;

import com.tomochi.activity.domain.SegmentPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UploadSegmentsRequest(
		@NotNull @Size(max = SegmentPolicy.MAX_SEGMENTS_PER_REQUEST) List<SegmentRequest> segments
) {
}
