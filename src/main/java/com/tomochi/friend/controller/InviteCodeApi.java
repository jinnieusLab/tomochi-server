package com.tomochi.friend.controller;

import com.tomochi.friend.dto.InviteCodeResponse;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.user.error.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "친구", description = "보여주기(작업 중 표시) · 반응하기(꾹) · 남기기(둘이 함께한 날). 메시지·순위는 없어요")
public interface InviteCodeApi {

	@Operation(summary = "초대 코드 재발급", description = """
			내 초대 코드를 새로 만들어요. **이전 코드는 바로 쓸 수 없어요** (이미 맺은 친구는 그대로예요).
			지금 코드는 `GET /api/v1/me`의 `inviteCode`로 볼 수 있어요. 코드는 만료되지 않아요.""")
	@ApiResponse(responseCode = "200", description = "새 초대 코드와 링크")
	@ApiErrorCodes(value = UserErrorCode.class, codes = "USER_NOT_FOUND")
	InviteCodeResponse regenerate(@CurrentUser AuthUser user);
}
