package com.tomochi.global.security;

import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.error.ErrorCode;
import com.tomochi.global.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증이 없거나 틀린 요청에 401을 {@code ErrorResponse} 형식으로 돌려줘요.
 * 앱은 {@code TOKEN_EXPIRED}면 Refresh Token으로 재발급하고, 나머지는 다시 로그인해요.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		errorResponseWriter.write(response, toErrorCode(exception));
	}

	private ErrorCode toErrorCode(AuthenticationException exception) {
		if (!(exception instanceof OAuth2AuthenticationException)) {
			return AuthErrorCode.UNAUTHENTICATED;
		}
		return isExpired(exception) ? AuthErrorCode.TOKEN_EXPIRED : AuthErrorCode.TOKEN_INVALID;
	}

	/**
	 * 만료는 JwtTimestampValidator가 "Jwt expired at ..." 오류로 알려줘요.
	 * 서명이 맞는 토큰에서만 만료를 검사하니, 위조 토큰이 TOKEN_EXPIRED로 보이지는 않아요.
	 */
	private boolean isExpired(Throwable exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof JwtValidationException validation) {
				return validation.getErrors().stream()
						.anyMatch(error -> error.getDescription() != null
								&& error.getDescription().startsWith("Jwt expired"));
			}
		}
		return false;
	}
}
