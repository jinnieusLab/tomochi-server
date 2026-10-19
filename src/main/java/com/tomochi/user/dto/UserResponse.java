package com.tomochi.user.dto;

import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import java.time.Instant;

/**
 * 내 정보. 친구에게 보이는 정보와 달리 초대 코드와 시간대까지 담아요.
 *
 * @param nickname   아직 정하지 않았으면 null
 * @param statusIcon 작업 중일 때 친구에게 보이는 아이콘 코드. 고르지 않았으면 null
 * @param timezone   IANA 시간대 이름 (예: Asia/Seoul). 하루·이번 주를 나누는 기준이에요
 */
public record UserResponse(
		Long id,
		String nickname,
		PetKind petKind,
		String statusIcon,
		String timezone,
		String inviteCode,
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
