package com.tomochi.realtime;

import com.tomochi.global.security.AuthUser;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.presence.dto.PresenceRequest;
import com.tomochi.presence.service.PresenceService;
import com.tomochi.squish.dto.PendingSquishesResponse;
import com.tomochi.squish.service.PendingSquishService;
import com.tomochi.squish.service.SquishDelivery;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code /api/v1/ws}. 메시지는 JSON {@code { type, data }}.
 *
 * <ul>
 *   <li>30초 동안 아무 메시지가 없으면 끊어요. 앱은 {@code ping}(또는 하트비트)을 보내 연결을 유지해요.
 *       앱이 비정상 종료돼도 연결이 남지 않아요 (10초마다 확인)</li>
 *   <li>Access Token이 만료되기 1분 전에 {@code token.expiring}을 보내고, 만료되면 끊어요. 앱은 재발급 후 다시 연결해요</li>
 *   <li>앱은 끊기면 1초, 2초, 4초 … 최대 60초 간격으로 다시 붙어요</li>
 * </ul>
 */
@Slf4j
@Component
public class RealtimeHandler extends TextWebSocketHandler {

	public static final CloseStatus TOKEN_EXPIRED = new CloseStatus(4001, "token expired");
	public static final CloseStatus IDLE = new CloseStatus(4000, "idle");
	static final Duration EXPIRY_WARNING = Duration.ofMinutes(1);
	static final Duration IDLE_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration IDLE_CHECK_INTERVAL = Duration.ofSeconds(10);
	private static final int MAX_MESSAGE_BYTES = 16 * 1024;
	private static final String LAST_ACTIVITY = "lastActivity";
	/** 모든 전송이 거치는 동기화된 세션. 원래 세션에 따로 쓰면 다른 스레드의 전송과 겹쳐 Tomcat이 거절해요. */
	private static final String SAFE_SESSION = "safeSession";

	private final LocalUserMessageSender sender;
	private final PresenceService presenceService;
	private final SquishDelivery squishDelivery;
	private final PendingSquishService pendingSquishService;
	private final JsonMapper jsonMapper;
	private final TaskScheduler taskScheduler;
	private final Clock clock;
	private final Map<String, ScheduledFuture<?>[]> timers = new java.util.concurrent.ConcurrentHashMap<>();

	public RealtimeHandler(LocalUserMessageSender sender, PresenceService presenceService, SquishDelivery squishDelivery,
			PendingSquishService pendingSquishService, JsonMapper jsonMapper,
			@Qualifier(RealtimeSchedulerConfig.REALTIME_SCHEDULER) TaskScheduler taskScheduler, Clock clock) {
		this.sender = sender;
		this.presenceService = presenceService;
		this.squishDelivery = squishDelivery;
		this.pendingSquishService = pendingSquishService;
		this.jsonMapper = jsonMapper;
		this.taskScheduler = taskScheduler;
		this.clock = clock;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		session.setTextMessageSizeLimit(MAX_MESSAGE_BYTES);
		touch(session);
		Long userId = userId(session);
		WebSocketSession safe = sender.register(userId, session);
		session.getAttributes().put(SAFE_SESSION, safe);
		scheduleTokenExpiry(session, safe);
		ScheduledFuture<?> idleCheck = taskScheduler.scheduleAtFixedRate(() -> closeIfIdle(session, safe),
				clock.instant().plus(IDLE_CHECK_INTERVAL), IDLE_CHECK_INTERVAL);
		timers.merge(session.getId(), new ScheduledFuture<?>[] {idleCheck}, RealtimeHandler::concat);
		sendPendingSquishes(userId, safe);
	}

	/** 연결되면 꺼져 있던 동안의 꾹을 친구별로 묶어 한 번 보내요. 앱은 알림을 한 번 띄우고 ack해요. */
	private void sendPendingSquishes(Long userId, WebSocketSession safe) {
		PendingSquishesResponse pending = pendingSquishService.findPending(userId);
		if (!pending.items().isEmpty()) {
			sender.sendTo(safe, toText("squish.pending", pending));
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		touch(session);
		RealtimeMessage incoming;
		try {
			incoming = jsonMapper.readValue(message.getPayload(), RealtimeMessage.class);
		} catch (JacksonException e) {
			reply(session, "error", Map.of("code", "MALFORMED_MESSAGE"));
			return;
		}
		switch (incoming.type() == null ? "" : incoming.type()) {
			case "ping" -> reply(session, "pong", Map.of());
			case "presence.update" -> handlePresence(session, incoming.data());
			case "squish.ack" -> handleSquishAck(session, incoming.data());
			default -> reply(session, "error", Map.of("code", "UNKNOWN_TYPE"));
		}
	}

	private void handlePresence(WebSocketSession session, JsonNode data) throws IOException {
		try {
			PresenceStatus status = PresenceStatus.valueOf(data.get("status").asString());
			JsonNode icon = data.get("icon");
			StatusIcon statusIcon = icon == null || icon.isNull() ? null : StatusIcon.valueOf(icon.asString());
			presenceService.heartbeat(authUser(session), new PresenceRequest(status, statusIcon));
		} catch (RuntimeException e) {
			reply(session, "error", Map.of("code", "INVALID_PRESENCE"));
		}
	}

	private void handleSquishAck(WebSocketSession session, JsonNode data) throws IOException {
		JsonNode ids = data == null ? null : data.get("squishIds");
		if (ids == null || !ids.isArray()) {
			reply(session, "error", Map.of("code", "INVALID_ACK"));
			return;
		}
		List<Long> squishIds = new ArrayList<>();
		ids.forEach(id -> squishIds.add(id.asLong()));
		squishDelivery.acknowledge(userId(session), squishIds);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sender.unregister(userId(session), session);
		try {
			presenceService.disconnected(authUser(session));
		} catch (RuntimeException e) {
			// 서버가 내려가는 중이면 Redis가 먼저 닫혀 있어요. 상태는 90초 TTL로 정리돼요.
			log.debug("연결 해제 후 상태 정리 실패: {}", e.getClass().getSimpleName());
		}
		ScheduledFuture<?>[] scheduled = timers.remove(session.getId());
		if (scheduled != null) {
			for (ScheduledFuture<?> future : scheduled) {
				future.cancel(false);
			}
		}
	}

	private void scheduleTokenExpiry(WebSocketSession session, WebSocketSession safe) {
		Instant expiresAt = (Instant) session.getAttributes().get(AuthHandshakeInterceptor.TOKEN_EXPIRES_AT);
		if (expiresAt == null || expiresAt.equals(Instant.MAX)) {
			return;
		}
		Instant warnAt = expiresAt.minus(EXPIRY_WARNING);
		ScheduledFuture<?> warning = taskScheduler.schedule(
				() -> sender.sendTo(safe, toText("token.expiring", Map.of("expiresAt", expiresAt.toString()))),
				warnAt.isAfter(clock.instant()) ? warnAt : clock.instant());
		ScheduledFuture<?> close = taskScheduler.schedule(() -> closeQuietly(safe, TOKEN_EXPIRED), expiresAt);
		timers.merge(session.getId(), new ScheduledFuture<?>[] {warning, close}, RealtimeHandler::concat);
	}

	private void touch(WebSocketSession session) {
		session.getAttributes().put(LAST_ACTIVITY, clock.instant());
	}

	private void closeIfIdle(WebSocketSession session, WebSocketSession safe) {
		Instant last = (Instant) session.getAttributes().get(LAST_ACTIVITY);
		if (last != null && last.plus(IDLE_TIMEOUT).isBefore(clock.instant())) {
			closeQuietly(safe, IDLE);
		}
	}

	private static ScheduledFuture<?>[] concat(ScheduledFuture<?>[] a, ScheduledFuture<?>[] b) {
		ScheduledFuture<?>[] merged = java.util.Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, merged, a.length, b.length);
		return merged;
	}

	private void reply(WebSocketSession session, String type, Object data) {
		WebSocketSession safe = (WebSocketSession) session.getAttributes().getOrDefault(SAFE_SESSION, session);
		sender.sendTo(safe, toText(type, data));
	}

	private TextMessage toText(String type, Object data) {
		return new TextMessage(jsonMapper.writeValueAsString(Map.of("type", type, "data", data)));
	}

	private static void closeQuietly(WebSocketSession session, CloseStatus status) {
		try {
			session.close(status);
		} catch (IOException e) {
			log.debug("WebSocket 닫기 실패: {}", e.getClass().getSimpleName());
		}
	}

	private static Long userId(WebSocketSession session) {
		return (Long) session.getAttributes().get(AuthHandshakeInterceptor.USER_ID);
	}

	private static AuthUser authUser(WebSocketSession session) {
		return new AuthUser(userId(session), (String) session.getAttributes().get(AuthHandshakeInterceptor.SESSION_ID));
	}
}
