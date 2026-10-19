package com.tomochi.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InviteCodeResponse(
		@Schema(description = "6자리 초대 코드. 헷갈리는 글자(0/O, 1/I/L)는 없어요", example = "K7QH2M")
		String inviteCode,

		@Schema(description = "친구에게 보낼 링크. 누르면 토모찌가 열려요", example = "tomochi://invite/K7QH2M")
		String inviteUrl
) {

	private static final String INVITE_URL_PREFIX = "tomochi://invite/";

	public static InviteCodeResponse of(String inviteCode) {
		return new InviteCodeResponse(inviteCode, INVITE_URL_PREFIX + inviteCode);
	}
}
