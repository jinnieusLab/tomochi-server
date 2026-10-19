package com.tomochi.realtime;

import com.tomochi.auth.service.TokenService;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 연결할 때 {@code Authorization: Bearer {accessToken}}으로 인증해요.
 * 토큰 검증은 다른 API처럼 Spring Security(Resource Server)가 핸드셰이크 요청에서 끝내고, 여기서는 결과를 꺼내 세션에 붙여요.
 */
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

	static final String USER_ID = "userId";
	static final String SESSION_ID = "sessionId";
	static final String TOKEN_EXPIRES_AT = "tokenExpiresAt";

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		attributes.put(USER_ID, Long.valueOf(jwt.getSubject()));
		attributes.put(SESSION_ID, jwt.getClaimAsString(TokenService.SESSION_ID_CLAIM));
		attributes.put(TOKEN_EXPIRES_AT, jwt.getExpiresAt() != null ? jwt.getExpiresAt() : Instant.MAX);
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}
}
