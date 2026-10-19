package com.tomochi.friend.controller;

import com.tomochi.friend.dto.AddFriendRequest;
import com.tomochi.friend.dto.FriendListResponse;
import com.tomochi.friend.dto.FriendResponse;
import com.tomochi.friend.dto.FriendSummaryResponse;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

	@Operation(summary = "친구 목록", description = """
			친구들과 지금 상태예요. 앱은 팝오버를 열 때와 열려 있는 동안 **30초마다** 불러요.
			- `presence.status`: WORKING · RESTING · AWAY · OFFLINE. 작업 중이면 `workingSince`로 "작업 중 · 1시간 25분"을 계산해요
			- 오프라인이면 `lastSeenAt`으로 "쉬는 중 · 3시간 전"을 보여줘요
			- 숨긴 친구는 빠지고 `hiddenCount`만 와요. `includeHidden=true`면 숨긴 친구도 `hidden: true`로 와요
			- 친구의 총 시간·받은 꾹 수는 주지 않아요 (비교하지 않아요)""")
	@ApiResponse(responseCode = "200", description = "친구 목록")
	FriendListResponse list(@CurrentUser AuthUser user,
			@Parameter(description = "숨긴 친구도 포함할지", example = "false") boolean includeHidden);

	@Operation(summary = "친구 프로필", description = """
			친구 목록의 한 줄과 같은 정보예요 (펫·닉네임·지금 상태). 숨긴 친구도 볼 수 있어요.
			둘이 함께한 날 잔디는 `GET /api/v1/friends/{friendId}/together`로 따로 받아요.
			친구가 아니거나 탈퇴했으면 404예요.""")
	@ApiResponse(responseCode = "200", description = "친구 프로필")
	@ApiErrorCodes(value = FriendErrorCode.class, codes = "FRIEND_NOT_FOUND")
	FriendSummaryResponse find(@CurrentUser AuthUser user,
			@Parameter(description = "친구 사용자 id", example = "34") Long friendId);

	@Operation(summary = "목록에서 숨기기", description = """
			내 쪽에서만 숨겨요. **상대는 몰라요.** 친구 관계와 둘이 함께한 날은 그대로 쌓여요.
			숨긴 친구가 보낸 꾹은 받지 않고(알림도 없어요), 숨긴 친구의 펫도 내 화면에 놀러 오지 않아요.
			여러 번 불러도 같아요.""")
	@ApiResponse(responseCode = "204", description = "숨김")
	@ApiErrorCodes(value = FriendErrorCode.class, codes = "FRIEND_NOT_FOUND")
	void hide(@CurrentUser AuthUser user, @Parameter(description = "친구 사용자 id", example = "34") Long friendId);

	@Operation(summary = "친구 끊기", description = """
			양쪽 모두에서 관계가 사라져요. **상대에게 알리지 않고** 상대 목록에서 조용히 사라져요.
			둘이 함께한 날 기록과 아직 안 받은 꾹도 지워져요. 다시 친구가 되려면 초대 코드를 다시 받아야 해요.""")
	@ApiResponse(responseCode = "204", description = "끊음")
	@ApiErrorCodes(value = FriendErrorCode.class, codes = "FRIEND_NOT_FOUND")
	void unfriend(@CurrentUser AuthUser user, @Parameter(description = "친구 사용자 id", example = "34") Long friendId);

	@Operation(summary = "다시 보이기", description = "숨긴 친구를 목록에 다시 보여요. 숨기지 않은 친구에게 불러도 괜찮아요.")
	@ApiResponse(responseCode = "204", description = "보임")
	@ApiErrorCodes(value = FriendErrorCode.class, codes = "FRIEND_NOT_FOUND")
	void unhide(@CurrentUser AuthUser user, @Parameter(description = "친구 사용자 id", example = "34") Long friendId);
}
