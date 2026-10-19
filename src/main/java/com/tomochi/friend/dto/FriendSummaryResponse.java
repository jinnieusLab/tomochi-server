package com.tomochi.friend.dto;

import com.tomochi.friend.repository.FriendQuery.FriendRow;
import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.user.domain.PetKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "친구 목록의 한 줄. 친구의 오늘 총 시간·받은 꾹 수 같은 비교 숫자는 넣지 않아요")
public record FriendSummaryResponse(
		@Schema(description = "친구 사용자 id", example = "34")
		Long id,

		@Schema(description = "닉네임. 정하지 않았으면 null", example = "민지", nullable = true)
		String nickname,

		@Schema(description = "펫 종류", example = "HOBBANG")
		PetKind petKind,

		@Schema(description = "지금 상태")
		PresenceView presence,

		@Schema(description = "오프라인일 때 마지막으로 접속한 시각 (5분 정밀도). 접속 중이면 null",
				example = "2026-10-17T04:12:00Z", nullable = true)
		Instant lastSeenAt,

		@Schema(description = "내가 목록에서 숨긴 친구인지. `includeHidden=true`일 때만 true가 와요", example = "false")
		boolean hidden
) {

	public static FriendSummaryResponse of(FriendRow row, Presence presence) {
		Instant lastSeenAt = presence.status() == PresenceStatus.OFFLINE ? row.lastSeenAt() : null;
		return new FriendSummaryResponse(row.id(), row.nickname(), row.petKind(), PresenceView.from(presence),
				lastSeenAt, row.hidden());
	}

	public record PresenceView(
			@Schema(description = "WORKING · RESTING · AWAY · OFFLINE", example = "WORKING")
			PresenceStatus status,

			@Schema(description = "작업 중일 때만 보이는 아이콘", example = "STUDY", nullable = true)
			StatusIcon icon,

			@Schema(description = "작업 중으로 바뀐 순간. \"작업 중 · 1시간 25분\"을 앱이 계산해요", example = "2026-10-17T07:40:00Z",
					nullable = true)
			Instant workingSince
	) {

		static PresenceView from(Presence presence) {
			return new PresenceView(presence.status(), presence.icon(), presence.workingSince());
		}
	}
}
