package com.tomochi.activity.controller;

import com.tomochi.activity.dto.UploadSegmentsRequest;
import com.tomochi.activity.dto.UploadSegmentsResponse;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "활동 기록", description = "토모찌와 함께한 시간. 서버는 합계가 아니라 작업 구간(시작·끝)을 받아 직접 계산해요")
public interface ActivityApi {

	@Operation(summary = "작업 구간 올리기", description = """
			앱은 **5분마다, 그리고 끌 때** 작업 구간을 모아 보내요 (한 번에 200개까지). 오프라인이면 쌓아 두었다가 나중에 보내요.

			- 작업 중이 시작되면 구간을 열고, 2분 무입력이 되면 닫아요 (끝 = 마지막 입력 시각)
			- 열린 구간은 "지금까지"로 잘라 보내고, 다음에 **같은 `clientSegmentId`로 늘려** 보내요
			- 같은 요청을 여러 번 보내도, 기기를 여러 대 켜 둬도 시간이 두 번 더해지지 않아요

			| 경우 | 처리 |
			| --- | --- |
			| 끝 ≤ 시작 | 그 구간만 거절 `INVALID_RANGE` |
			| 끝이 서버 시각 + 2분보다 뒤 | 서버 시각으로 잘라 저장 (`adjusted`) |
			| 시작부터 미래 | 거절 `IN_FUTURE` |
			| 시작이 7일보다 전 | 거절 `TOO_OLD` |
			| 12시간보다 김 | 거절 `TOO_LONG` |
			| `clientSegmentId`가 UUID가 아님 | 거절 `INVALID_ID` |

			거절된 구간은 다시 보내지 말고 버려요. 하루 합계가 20시간을 넘는 부분은 조회할 때 잘라요.""")
	@ApiResponse(responseCode = "200", description = "저장 결과. 일부가 거절돼도 200이에요")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = {"INVALID_INPUT", "MALFORMED_REQUEST"})
	UploadSegmentsResponse upload(@CurrentUser AuthUser user, UploadSegmentsRequest request);
}
