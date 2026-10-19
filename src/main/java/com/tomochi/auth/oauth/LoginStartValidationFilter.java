package com.tomochi.auth.oauth;

import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.auth.service.Pkce;
import com.tomochi.global.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 로그인 시작({@code GET /api/v1/auth/oauth2/{provider}})에 S256 challenge가 없으면 제공자로 보내지 않아요.
 * challenge 없이 로그인하면 나중에 코드를 토큰으로 바꿀 방법이 없어서, 처음부터 막아요.
 */
@RequiredArgsConstructor
public class LoginStartValidationFilter extends OncePerRequestFilter {

	private static final RequestMatcher LOGIN_START = PathPatternRequestMatcher.withDefaults()
			.matcher(HttpMethod.GET, LoginRequestAttributes.AUTHORIZATION_BASE_URI + "/{registrationId}");

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !LOGIN_START.matches(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!Pkce.isValidChallenge(request.getParameter(LoginRequestAttributes.CHALLENGE))) {
			errorResponseWriter.write(response, AuthErrorCode.INVALID_CHALLENGE);
			return;
		}
		chain.doFilter(request, response);
	}
}
