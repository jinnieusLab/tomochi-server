package com.tomochi.auth.controller;

import com.tomochi.auth.dto.RefreshRequest;
import com.tomochi.auth.dto.TokenRequest;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.service.AuthCodeService;
import com.tomochi.auth.service.RefreshTokenRotationService;
import com.tomochi.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthCodeService authCodeService;
	private final TokenService tokenService;
	private final RefreshTokenRotationService refreshTokenRotationService;

	@Operation(
			summary = "1회용 코드를 토큰으로 바꾸기",
			description = """
					로그인은 브라우저로 `GET /api/v1/auth/oauth2/{google|github}?challenge=...&petKind=...`를 열어 시작해요.
					끝나면 `tomochi://auth/callback?code=...`로 앱이 열리고, 그 코드와 로그인을 시작할 때 만든
					`codeVerifier`를 여기에 보내 토큰을 받아요.""")
	@PostMapping("/token")
	public TokenResponse token(@Valid @RequestBody TokenRequest request) {
		Long userId = authCodeService.consume(request.code(), request.codeVerifier());
		return tokenService.issueForNewLogin(userId, request.deviceNameOrDefault());
	}

	@Operation(
			summary = "토큰 재발급",
			description = """
					Access Token이 만료되면(`TOKEN_EXPIRED`) Refresh Token으로 새 토큰 한 쌍을 받아요. 쓴 Refresh Token은 바로 폐기돼요.
					- `401 REFRESH_TOKEN_INVALID`: 없거나 만료됨 → 다시 로그인
					- `409 REFRESH_TOKEN_ROTATED`: 같은 토큰으로 보낸 다른 요청이 방금 성공함 → 그 요청이 받은 토큰을 사용
					- `403 REFRESH_TOKEN_REUSED`: 폐기된 토큰 재사용(탈취 의심) → 이 기기 토큰이 모두 끊김, 다시 로그인""")
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return refreshTokenRotationService.rotate(request.refreshToken());
	}
}
