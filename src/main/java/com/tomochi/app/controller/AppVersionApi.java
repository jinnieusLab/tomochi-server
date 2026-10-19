package com.tomochi.app.controller;

import com.tomochi.app.dto.AppVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "앱", description = "맥 앱 버전 안내. 모든 요청에 `X-Tomochi-Version` 헤더를 보내면, 최소 버전보다 낮을 때 426을 받아요")
public interface AppVersionApi {

	@Operation(summary = "앱 버전 확인", description = """
			로그인 없이 불러요. 앱이 켜질 때 한 번 확인해요.
			- 앱 버전 < `minimum`: 꼭 업데이트해야 해요. 다른 API는 `426 APP_UPDATE_REQUIRED`를 받아요
			- 앱 버전 < `latest`: 조용히 업데이트를 권하면 돼요
			이 API는 옛 앱이라도 늘 응답해요.""")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "버전 정보")
	AppVersionResponse version();
}
