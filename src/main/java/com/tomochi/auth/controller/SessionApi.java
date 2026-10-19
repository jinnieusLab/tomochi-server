package com.tomochi.auth.controller;

import com.tomochi.auth.dto.SessionResponse;
import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "인증")
public interface SessionApi {

	@Operation(summary = "로그아웃",
			description = "지금 기기의 Refresh Token을 모두 폐기해요. 이미 받은 Access Token은 만료(최대 30분)까지 유효해요. "
					+ "여러 번 불러도 같아요.")
	@ApiResponse(responseCode = "204", description = "로그아웃됨")
	void logout(@CurrentUser AuthUser user);

	@Operation(summary = "로그인한 기기 목록", description = "최근에 쓴 기기부터 와요. `current: true`가 지금 기기예요.")
	@ApiResponse(responseCode = "200", description = "기기 목록")
	List<SessionResponse> sessions(@CurrentUser AuthUser user);

	@Operation(summary = "다른 기기 로그아웃", description = "그 기기의 Refresh Token을 모두 폐기해요. 지금 기기 id를 보내면 로그아웃과 같아요.")
	@ApiResponse(responseCode = "204", description = "로그아웃됨")
	@ApiErrorCodes(value = AuthErrorCode.class, codes = "SESSION_NOT_FOUND")
	void revoke(@CurrentUser AuthUser user,
			@Parameter(description = "`GET /api/v1/auth/sessions`의 `id`", example = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b")
			String sessionId);
}
