package com.tomochi.squish.controller;

import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.squish.error.SquishErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "꾹", description = "친구에게 보내는 반응 하나. 친구 화면의 펫이 소리 없이 출렁이고 로컬 알림이 떠요")
public interface SquishApi {

	@Operation(summary = "꾹 보내기", description = """
			- **같은 친구에게는 10초에 한 번**이에요. 대기 중이면 429와 `Retry-After`(남은 초)가 와요
			- 쉬는 중·자리 비움·오프라인인 친구에게도 보낼 수 있어요 (앱이 꺼져 있으면 켜질 때 모아서 받아요)
			- 친구가 아니면 404예요
			- 남용을 막으려고 1분에 30번까지 보낼 수 있어요""")
	@ApiResponse(responseCode = "202", description = "보냄")
	@ApiErrorCodes(value = FriendErrorCode.class, codes = "FRIEND_NOT_FOUND")
	@ApiErrorCodes(value = SquishErrorCode.class, codes = "SQUISH_COOLDOWN")
	void send(@CurrentUser AuthUser user, @Parameter(description = "친구 사용자 id", example = "34") Long friendId);
}
