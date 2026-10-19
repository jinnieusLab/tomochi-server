package com.tomochi.friend.dto;

import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 한 명. 친구의 총 시간·꾹 수 같은 비교 숫자는 없어요")
public record FriendResponse(
		@Schema(description = "친구 사용자 id", example = "34")
		Long id,

		@Schema(description = "닉네임. 정하지 않았으면 null", example = "민지", nullable = true)
		String nickname,

		@Schema(description = "펫 종류", example = "HOBBANG")
		PetKind petKind
) {

	public static FriendResponse from(User user) {
		return new FriendResponse(user.getId(), user.getNickname(), user.getPetKind());
	}
}
