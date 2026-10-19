package com.tomochi.presence.controller;

import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.presence.dto.PresenceRequest;
import com.tomochi.presence.dto.StatusIconRequest;
import com.tomochi.user.error.UserErrorCode;
import com.tomochi.presence.error.PresenceErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "상태", description = "작업 중 / 쉬는 중 / 자리 비움. 친구 목록의 '작업 중 · 1시간 25분'이 여기서 나와요")
public interface PresenceApi {

	@Operation(summary = "내 상태 보내기 (하트비트)", description = """
			상태가 바뀔 때 바로, 바뀌지 않아도 **30초마다** 보내요. 90초 동안 오지 않으면 그 기기는 오프라인이에요.
			- 기기(로그인 세션)마다 따로 기억하고, 하나라도 작업 중이면 친구에게는 작업 중으로 보여요
			- 작업 중으로 바뀐 순간부터 "작업 중 · N분"을 세요. 계속 작업 중이면 시작 시각은 그대로예요
			- 아이콘은 작업 중일 때만 친구에게 보여요. 자리 비움(15분)이 되면 앱이 `icon: null`로 보내요""")
	@ApiResponse(responseCode = "204", description = "받음")
	@ApiErrorCodes(value = PresenceErrorCode.class, codes = "INVALID_PRESENCE_STATUS")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = {"INVALID_INPUT", "MALFORMED_REQUEST"})
	void heartbeat(@CurrentUser AuthUser user, PresenceRequest request);

	@Operation(summary = "상태 아이콘 바꾸기", description = """
			팝오버의 아이콘 트레이에서 고른 아이콘을 저장해요. 접속 중인 기기의 상태에도 바로 반영돼 친구에게 보여요.
			`null`이면 아이콘을 지워요. 아이콘은 작업 중일 때만 친구에게 보여요.""")
	@ApiResponse(responseCode = "204", description = "바뀜")
	@ApiErrorCodes(value = UserErrorCode.class, codes = "USER_NOT_FOUND")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "MALFORMED_REQUEST")
	void changeStatusIcon(@CurrentUser AuthUser user, StatusIconRequest request);
}
