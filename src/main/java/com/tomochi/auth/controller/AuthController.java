package com.tomochi.auth.controller;

import com.tomochi.auth.dto.RefreshRequest;
import com.tomochi.auth.dto.TokenRequest;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.service.AuthCodeService;
import com.tomochi.auth.service.RefreshTokenRotationService;
import com.tomochi.auth.service.TokenService;
import com.tomochi.global.error.BusinessException;
import com.tomochi.global.error.CommonErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

	private final AuthCodeService authCodeService;
	private final TokenService tokenService;
	private final RefreshTokenRotationService refreshTokenRotationService;

	/**
	 * Spring Security의 OAuth2 필터가 이 주소를 먼저 가로채서, 등록된 제공자면 여기까지 오지 않아요. Swagger 문서용이에요.
	 * 여기까지 왔다면 모르는 제공자라 404예요.
	 */
	@Override
	@GetMapping("/oauth2/{provider}")
	public void startLogin(@PathVariable String provider, @RequestParam String challenge,
			@RequestParam(required = false) String petKind) {
		throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
	}

	@Override
	@PostMapping("/token")
	public TokenResponse token(@Valid @RequestBody TokenRequest request) {
		Long userId = authCodeService.consume(request.code(), request.codeVerifier());
		return tokenService.issueForNewLogin(userId, request.deviceNameOrDefault());
	}

	@Override
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return refreshTokenRotationService.rotate(request.refreshToken());
	}
}
