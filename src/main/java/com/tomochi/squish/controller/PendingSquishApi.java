package com.tomochi.squish.controller;

import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.squish.dto.AckSquishesRequest;
import com.tomochi.squish.dto.PendingSquishesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "꾹")
public interface PendingSquishApi {

	@Operation(summary = "안 받은 꾹", description = """
			앱이 꺼져 있던 동안(또는 WebSocket이 끊겨 있던 동안) 받은 꾹을 친구별로 묶어 줘요. 30일이 지난 꾹은 없어요.
			WebSocket에 연결되면 같은 내용이 `squish.pending`으로 한 번 와서, 폴링 단계에서만 쓰면 돼요.
			받은 뒤에는 `POST /api/v1/squishes/ack`로 알려 주세요. 알리지 않으면 다음에 또 와요.""")
	@ApiResponse(responseCode = "200", description = "친구별 안 받은 꾹")
	PendingSquishesResponse pending(@CurrentUser AuthUser user);

	@Operation(summary = "꾹 받음 알리기", description = """
			받은 꾹 id를 보내면 전달 완료로 표시해요. 내가 받은 꾹만 바뀌고, 여러 번 보내도 같아요.
			WebSocket에서는 `squish.ack` 메시지로 같은 일을 해요.""")
	@ApiResponse(responseCode = "204", description = "표시함")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	void acknowledge(@CurrentUser AuthUser user, AckSquishesRequest request);
}
