package com.tomochi.realtime;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;

/**
 * 이 서버에 연결된 세션들. 한 사용자가 기기 여러 대로 연결할 수 있어요.
 * 여러 스레드(꾹 API, 배치)가 같은 세션에 동시에 쓰지 않게 {@link ConcurrentWebSocketSessionDecorator}로 감싸요.
 */
@Slf4j
@Component
public class LocalUserMessageSender implements UserMessageSender {

	private static final int SEND_TIME_LIMIT_MILLIS = 5_000;
	private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

	private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
	private final JsonMapper jsonMapper;

	public LocalUserMessageSender(JsonMapper jsonMapper, MeterRegistry meterRegistry) {
		this.jsonMapper = jsonMapper;
		Gauge.builder("tomochi.websocket.sessions", this, LocalUserMessageSender::sessionCount)
				.description("지금 연결된 WebSocket 세션 수")
				.register(meterRegistry);
	}

	public WebSocketSession register(Long userId, WebSocketSession session) {
		WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT);
		sessions.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(safe);
		return safe;
	}

	public void unregister(Long userId, WebSocketSession session) {
		sessions.computeIfPresent(userId, (id, set) -> {
			set.removeIf(registered -> registered.getId().equals(session.getId()));
			return set.isEmpty() ? null : set;
		});
	}

	@Override
	public boolean send(Long userId, String type, Object data) {
		Set<WebSocketSession> targets = sessions.get(userId);
		if (targets == null || targets.isEmpty()) {
			return false;
		}
		TextMessage message = new TextMessage(jsonMapper.writeValueAsString(Map.of("type", type, "data", data)));
		boolean sent = false;
		for (WebSocketSession session : targets) {
			sent |= sendTo(session, message);
		}
		return sent;
	}

	boolean sendTo(WebSocketSession session, TextMessage message) {
		if (!session.isOpen()) {
			return false;
		}
		try {
			session.sendMessage(message);
			return true;
		} catch (IOException | IllegalStateException e) {
			log.debug("WebSocket 전송 실패: {}", e.getClass().getSimpleName());
			return false;
		}
	}

	public boolean isConnected(Long userId) {
		Set<WebSocketSession> targets = sessions.get(userId);
		return targets != null && targets.stream().anyMatch(WebSocketSession::isOpen);
	}

	private double sessionCount() {
		return sessions.values().stream().mapToInt(Set::size).sum();
	}
}
