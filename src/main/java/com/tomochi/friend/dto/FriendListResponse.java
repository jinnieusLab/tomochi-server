package com.tomochi.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FriendListResponse(
		@Schema(description = "친구들. 정렬(작업 중 / 쉬는 중 나누기)은 앱이 해요")
		List<FriendSummaryResponse> friends,

		@Schema(description = "목록에서 숨긴 친구 수", example = "1")
		int hiddenCount
) {
}
