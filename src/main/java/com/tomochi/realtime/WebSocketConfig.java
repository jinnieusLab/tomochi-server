package com.tomochi.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 순수 WebSocket + JSON. STOMP는 Spring 쪽은 편하지만 Swift에 쓸 만한 클라이언트가 마땅치 않고,
 * 메시지 종류가 몇 개 안 돼서 {@code URLSessionWebSocketTask}로 바로 붙는 쪽이 단순해요.
 * 브라우저가 아니라 맥 앱만 붙어서 Origin 검사는 하지 않아요(인증은 Bearer 토큰).
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

	public static final String PATH = "/api/v1/ws";

	private final RealtimeHandler realtimeHandler;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(realtimeHandler, PATH)
				.addInterceptors(new AuthHandshakeInterceptor())
				.setAllowedOriginPatterns("*");
	}
}
