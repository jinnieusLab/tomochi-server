package com.tomochi.user.dto;

import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "내 정보. 친구에게 보이는 정보와 달리 초대 코드와 시간대까지 담아요")
public record UserResponse(
		@Schema(description = "사용자 id", example = "12")
		Long id,

		@Schema(description = "닉네임. 아직 정하지 않았으면 null", example = "민지", nullable = true)
		String nickname,

		@Schema(description = "펫 종류", example = "HOBBANG")
		PetKind petKind,

		@Schema(description = "작업 중일 때 친구에게 보이는 아이콘 코드. 고르지 않았으면 null", example = "STUDY", nullable = true)
		StatusIcon statusIcon,

		@Schema(description = "IANA 시간대. 하루·이번 주를 나누는 기준", example = "Asia/Seoul")
		String timezone,

		@Schema(description = "친구 초대 코드 (6자리)", example = "K7QH2M")
		String inviteCode,

		@Schema(description = "가입 시각 (UTC)", example = "2026-10-01T09:12:00Z")
		Instant createdAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getNickname(),
				user.getPetKind(),
				user.getStatusIcon(),
				user.getTimezone(),
				user.getInviteCode(),
				user.getCreatedAt());
	}
}
