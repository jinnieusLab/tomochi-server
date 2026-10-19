package com.tomochi.global.ratelimit;

import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API 요청 수를 제한해요. 인증 필터 뒤에 둬서 로그인한 요청은 사용자 id로, 아니면 IP로 세요.
 * IP는 Nginx가 넘긴 X-Forwarded-For로 Tomcat이 바꿔 둔 원래 주소예요 ({@code forward-headers-strategy: native}).
 * Redis가 응답하지 않으면 막지 않고 통과시켜요. 제한 때문에 서비스 전체가 멈추면 안 돼요.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	/** 필터 체인에서 맡는 자리 */
	public enum Stage {
		/** 로그인 시작({@code /api/v1/auth/oauth2/**}). OAuth2 필터가 응답해 버리기 전에 세요 */
		LOGIN_START,
		/** 나머지 API. Bearer 인증 뒤라 사용자 id를 알아요 */
		API
	}


	private static final String API_PREFIX = "/api/v1/";
	private static final String AUTH_PREFIX = "/api/v1/auth/oauth2/";

	private final Stage stage;
	private final RateLimiter rateLimiter;
	private final RateLimitProperties properties;
	private final ErrorResponseWriter errorResponseWriter;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (!properties.enabled() || !uri.startsWith(API_PREFIX)) {
			return true;
		}
		boolean loginStart = uri.startsWith(AUTH_PREFIX);
		return stage == Stage.LOGIN_START ? !loginStart : loginStart;
	}

	/** 두 자리에 같은 클래스를 두니, "이미 돌았음" 표시를 자리마다 따로 써요. */
	@Override
	protected String getAlreadyFilteredAttributeName() {
		return RateLimitFilter.class.getName() + "." + stage + ".FILTERED";
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Rule rule = ruleFor(request);
		RateLimiter.Result result;
		try {
			result = rateLimiter.acquire(rule.key(), rule.limit(), properties.window());
		} catch (RuntimeException e) {
			log.warn("요청 제한 확인 실패, 통과시켜요: {}", e.getClass().getSimpleName());
			chain.doFilter(request, response);
			return;
		}
		if (!result.allowed()) {
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
			errorResponseWriter.write(response, CommonErrorCode.RATE_LIMITED, result.retryAfterSeconds());
			return;
		}
		chain.doFilter(request, response);
	}

	private Rule ruleFor(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String method = request.getMethod();
		String ip = request.getRemoteAddr();
		if (uri.startsWith(AUTH_PREFIX)
				|| (HttpMethod.POST.matches(method)
				&& (uri.equals("/api/v1/auth/token") || uri.equals("/api/v1/auth/refresh")))) {
			return new Rule("rl:auth:" + ip, properties.authPerIp());
		}
		String subject = currentUserId();
		String who = subject != null ? "u" + subject : "ip" + ip;
		if (HttpMethod.POST.matches(method) && uri.equals("/api/v1/activity/segments")) {
			return new Rule("rl:upload:" + who, properties.uploadPerUser());
		}
		return new Rule("rl:api:" + who, properties.defaultPerUser());
	}

	private static String currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof Jwt jwt ? jwt.getSubject() : null;
	}

	private record Rule(String key, int limit) {
	}
}
