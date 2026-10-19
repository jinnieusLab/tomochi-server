package com.tomochi.activity.dto;

import com.tomochi.activity.domain.RejectReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UploadSegmentsResponse(
		@Schema(description = "저장한 구간 수 (잘라서 저장한 것 포함)", example = "3")
		int accepted,

		@Schema(description = "미래 시각을 서버 시각으로 잘라 저장한 구간 수", example = "0")
		int adjusted,

		@Schema(description = "받지 않은 구간. 앱은 다시 보내지 않고 버려요")
		List<Rejected> rejected
) {

	public record Rejected(
			@Schema(description = "앱이 보낸 id 그대로", example = "8f6c2a1e-4b7d-4c3a-9f10-2d5e6a7b8c9d")
			String clientSegmentId,

			@Schema(description = "거절 이유", example = "TOO_OLD")
			RejectReason reason
	) {
	}
}
