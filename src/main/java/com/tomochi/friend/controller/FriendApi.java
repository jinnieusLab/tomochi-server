package com.tomochi.friend.controller;

import com.tomochi.friend.dto.AddFriendRequest;
import com.tomochi.friend.dto.FriendResponse;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "친구")
public interface FriendApi {

	@Operation(summary = "초대 코드로 친구 맺기", description = """
			친구의 초대 코드(또는 `tomochi://invite/{code}` 링크의 코드)로 바로 친구가 돼요. 수락 단계는 없어요.
			- 이미 친구면 그대로 200이에요 (여러 번 보내도 같아요)
			- 친구는 최대 20명. 나나 상대가 20명이면 409
			- 코드 맞히기를 막으려고 1분에 10번, 하루 50번까지 시도할 수 있어요 (넘으면 429)""")
	@ApiResponse(responseCode = "200", description = "새 친구 (또는 이미 친구)")
	@ApiErrorCodes(value = FriendErrorCode.class,
			codes = {"INVITE_CODE_NOT_FOUND", "OWN_INVITE_CODE", "FRIEND_LIMIT_EXCEEDED"})
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	FriendResponse add(@CurrentUser AuthUser user, AddFriendRequest request);
}
