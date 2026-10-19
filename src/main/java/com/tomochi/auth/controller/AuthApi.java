package com.tomochi.auth.controller;

import com.tomochi.auth.dto.RefreshRequest;
import com.tomochi.auth.dto.TokenRequest;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.docs.ApiErrorCodes;
import com.tomochi.global.error.CommonErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "소셜 로그인(PKCE 방식 1회용 코드)과 토큰 재발급. 흐름은 docs/oauth.md")
public interface AuthApi {

	@Operation(
			summary = "소셜 로그인 시작 (브라우저로 열기)",
			description = """
					맥 앱이 **브라우저로** 여는 주소예요. 서버가 Google·GitHub 로그인 페이지로 보내고,
					로그인이 끝나면 `tomochi://auth/callback?code=…`로 앱을 다시 열어요 (실패하면 `?error=LOGIN_FAILED`).

					1. 앱이 무작위 `codeVerifier`(43~128자)를 만들어 보관해요.
					2. `challenge = BASE64URL(SHA256(codeVerifier))` (패딩 없는 43자)
					3. 이 주소를 `?challenge=…&petKind=…`로 열어요.
					4. 받은 `code`와 `codeVerifier`로 `POST /api/v1/auth/token`을 불러요.

					처음 로그인하면 계정이 만들어지고, 펫은 `petKind`(없거나 모르면 CHOCO)예요.
					실제 처리는 Spring Security 필터가 해서 이 API로 문서만 보여 줘요.""")
	@SecurityRequirements
	@ApiResponse(responseCode = "302", description = "제공자 로그인 페이지로 이동")
	@ApiErrorCodes(value = AuthErrorCode.class, codes = "INVALID_CHALLENGE")
	void startLogin(
			@Parameter(in = ParameterIn.PATH, description = "로그인 제공자",
					schema = @Schema(allowableValues = {"google", "github"})) String provider,
			@Parameter(description = "S256 code_challenge (Base64URL, 패딩 없음, 43자)", required = true,
					example = "uVzdJH4DNvZMUVLwHRcytsh0mwMr9gBB_R7DxseoPrw") String challenge,
			@Parameter(description = "첫 로그인 때 만들 펫. 이미 계정이 있으면 무시돼요",
					schema = @Schema(allowableValues = {"CHOCO", "BUTTER", "HOBBANG", "SHERBET", "PEANUT", "HONEYCOMB"}))
			String petKind);

	@Operation(
			summary = "1회용 코드를 토큰으로 바꾸기",
			description = """
					로그인 직후 받은 `code`와 로그인을 시작할 때 만든 `codeVerifier`를 보내요.
					코드는 60초 동안 한 번만 쓸 수 있고, verifier가 틀려도 코드는 버려져요.""")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "토큰 발급")
	@ApiErrorCodes(value = AuthErrorCode.class, codes = "AUTH_CODE_INVALID")
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	TokenResponse token(TokenRequest request);

	@Operation(
			summary = "토큰 재발급",
			description = """
					Access Token이 만료되면(`TOKEN_EXPIRED`) Refresh Token으로 새 토큰 한 쌍을 받아요.
					쓴 Refresh Token은 바로 폐기되니 응답으로 받은 새 값을 저장해요.
					- `401 REFRESH_TOKEN_INVALID`: 없거나 만료됐거나 로그아웃된 기기 → 다시 로그인
					- `409 REFRESH_TOKEN_ROTATED`: 같은 토큰으로 보낸 다른 요청이 방금 성공 → 그 요청이 받은 토큰을 사용
					- `403 REFRESH_TOKEN_REUSED`: 폐기된 토큰 재사용(탈취 의심) → 이 기기 토큰이 모두 끊김, 다시 로그인""")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "새 토큰 한 쌍")
	@ApiErrorCodes(value = AuthErrorCode.class,
			codes = {"REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_ROTATED", "REFRESH_TOKEN_REUSED"})
	@ApiErrorCodes(value = CommonErrorCode.class, codes = "INVALID_INPUT")
	TokenResponse refresh(RefreshRequest request);
}
