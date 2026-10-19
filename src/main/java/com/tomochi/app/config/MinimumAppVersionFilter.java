package com.tomochi.app.config;

import com.tomochi.app.domain.AppVersion;
import com.tomochi.app.error.AppErrorCode;
import com.tomochi.global.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code X-Tomochi-Version}이 최소 버전보다 낮으면 426으로 막아요.
 * 인증보다 먼저 돌아서, 옛 앱은 401 대신 "업데이트하세요"를 받아요.
 * <ul>
 *   <li>헤더가 없으면 통과 (브라우저 로그인, Swagger)</li>
 *   <li>형식이 틀리면 통과. 버전을 못 읽는 건 앱 버그라 막아도 고칠 방법이 없어요</li>
 *   <li>버전 확인 API는 늘 통과. 막히면 앱이 무엇을 받아야 할지 알 수 없어요</li>
 * </ul>
 */
@RequiredArgsConstructor
public class MinimumAppVersionFilter extends OncePerRequestFilter {

	public static final String VERSION_HEADER = "X-Tomochi-Version";
	private static final String VERSION_PATH = "/api/v1/app/version";

	private final AppVersionProperties properties;
	private final ErrorResponseWriter errorResponseWriter;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return VERSION_PATH.equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Optional<AppVersion> appVersion = AppVersion.tryParse(request.getHeader(VERSION_HEADER));
		if (appVersion.isPresent() && appVersion.get().isOlderThan(properties.minimumVersion())) {
			errorResponseWriter.write(response, AppErrorCode.APP_UPDATE_REQUIRED);
			return;
		}
		chain.doFilter(request, response);
	}
}
