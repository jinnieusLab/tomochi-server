package com.tomochi.user.controller;

import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.user.dto.UpdateUserRequest;
import com.tomochi.user.dto.UserResponse;
import com.tomochi.user.error.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "내 정보", description = "로그인한 사용자 자신의 정보. 친구에게 보이는 정보는 친구 API에서 따로 받아요")
public interface UserApi {

	@Operation(summary = "내 정보 조회", description = "앱이 켜질 때와 로그인 직후에 불러요.")
	@ApiResponse(responseCode = "200", description = "내 정보")
	@ApiErrorCodes(value = UserErrorCode.class, codes = "USER_NOT_FOUND")
	UserResponse find(@CurrentUser AuthUser user);

	@Operation(summary = "내 정보 바꾸기", description = """
			**보낸 필드만** 바꿔요. 보내지 않은 필드는 그대로예요. 여러 필드를 한 번에 보내도 되고,
			하나라도 틀리면 아무것도 바뀌지 않아요.

			| 필드 | 규칙 | 지우기 |
			| --- | --- | --- |
			| `nickname` | 앞뒤 공백을 지우고 1~12자 (이모지 1개 = 1자), 줄바꿈·제어 문자 금지, 중복 허용 | `null` 또는 빈 문자열 |
			| `petKind` | `CHOCO` `BUTTER` `HOBBANG` `SHERBET` `PEANUT` `HONEYCOMB` | 불가 |
			| `timezone` | IANA 이름 (예: `Asia/Seoul`). `+09:00`, `KST`는 안 돼요 | 불가 |

			앱은 켜질 때 맥의 시간대가 `timezone`과 다르면 보내요.""")
	@ApiResponse(responseCode = "200", description = "바뀐 뒤의 내 정보")
	@ApiErrorCodes(value = UserErrorCode.class,
			codes = {"INVALID_NICKNAME", "INVALID_PET_KIND", "INVALID_TIMEZONE", "USER_NOT_FOUND"})
	UserResponse update(@CurrentUser AuthUser user, UpdateUserRequest request);
}
