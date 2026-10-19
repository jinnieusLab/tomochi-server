package com.tomochi.auth.oauth;

import com.tomochi.auth.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 사용자가 로그인을 취소했거나 state가 맞지 않으면 앱으로 돌아가 오류를 알려요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	static final String LOGIN_FAILED = "LOGIN_FAILED";

	private final AuthProperties authProperties;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		// 제공자가 준 설명에는 요청 값이 섞일 수 있어서 오류 코드만 남겨요.
		String reason = exception instanceof OAuth2AuthenticationException oauth2
				? oauth2.getError().getErrorCode()
				: exception.getClass().getSimpleName();
		log.info("소셜 로그인 실패: {}", reason);
		response.sendRedirect(UriComponentsBuilder.fromUriString(authProperties.appRedirectUri())
				.queryParam("error", LOGIN_FAILED)
				.build()
				.toUriString());
	}
}
