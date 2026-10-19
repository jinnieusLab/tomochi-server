package com.tomochi.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 인증 설정. {@code jwtSecret}만 환경 변수(JWT_SECRET)로 넣고 나머지는 기본값을 써요.
 *
 * @param jwtSecret       HS256 서명 키. Base64로 인코딩한 32바이트 이상 값 ({@code openssl rand -base64 48})
 * @param accessTokenTtl  Access Token 수명. 블랙리스트가 없어서 로그아웃해도 이만큼은 유효해요
 * @param refreshTokenTtl Refresh Token 수명. 몇 주씩 켜 두는 앱이라 길게 둬요
 * @param authCodeTtl     로그인 직후 앱에 넘기는 1회용 코드 수명
 * @param appRedirectUri  로그인이 끝나면 돌아갈 맥 앱 주소
 */
@ConfigurationProperties("tomochi.auth")
public record AuthProperties(
		String jwtSecret,
		@DefaultValue("tomochi") String issuer,
		@DefaultValue("30m") Duration accessTokenTtl,
		@DefaultValue("30d") Duration refreshTokenTtl,
		@DefaultValue("60s") Duration authCodeTtl,
		@DefaultValue("tomochi://auth/callback") String appRedirectUri
) {
}
