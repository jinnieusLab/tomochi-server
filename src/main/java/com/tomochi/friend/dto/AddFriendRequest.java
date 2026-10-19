package com.tomochi.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddFriendRequest(
		@Schema(description = "친구의 초대 코드. 대소문자·앞뒤 공백은 상관없어요", example = "K7QH2M")
		@NotBlank @Size(max = 20) String inviteCode
) {
}
