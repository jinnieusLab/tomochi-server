package com.tomochi.presence.service;

import com.tomochi.global.error.BusinessException;
import com.tomochi.global.security.AuthUser;
import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.dto.PresenceRequest;
import com.tomochi.presence.error.PresenceErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 하트비트. 앱은 상태가 바뀔 때 바로, 바뀌지 않아도 30초마다 보내요.
 *
 * <p>"쉬는 중 · 3시간 전"에 쓰는 {@code users.last_seen_at}은 오프라인이 되는 순간을 잡는 대신(키 만료 알림이 필요해요)
 * 하트비트 때 남겨요. 30초마다 DB에 쓰지 않으려고 저장된 값이 5분보다 오래됐을 때만 바꾸는 조건부 UPDATE를 써요.
 * "3시간 전"을 보여주는 데는 5분 정밀도면 충분해요.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

	static final Duration LAST_SEEN_PRECISION = Duration.ofMinutes(5);

	private final PresenceStore presenceStore;
	private final PresenceNotifier presenceNotifier;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public void heartbeat(AuthUser user, PresenceRequest request) {
		if (request.status() == PresenceStatus.OFFLINE) {
			throw new BusinessException(PresenceErrorCode.INVALID_PRESENCE_STATUS);
		}
		Instant now = clock.instant();
		Presence before = presenceStore.find(user.userId(), now);
		presenceStore.update(user.userId(), user.sessionId(), request.status(), request.icon(), now);
		presenceNotifier.notifyIfChanged(user.userId(), before, presenceStore.find(user.userId(), now));
		touchLastSeen(user.userId(), now);
	}

	/** 기기의 실시간 연결이 끊겼어요. 그 기기 상태를 바로 지우고, 친구에게 보이는 값이 바뀌었으면 알려요. */
	public void disconnected(AuthUser user) {
		Instant now = clock.instant();
		Presence before = presenceStore.find(user.userId(), now);
		presenceStore.remove(user.userId(), user.sessionId());
		presenceNotifier.notifyIfChanged(user.userId(), before, presenceStore.find(user.userId(), now));
	}

	private void touchLastSeen(Long userId, Instant now) {
		LocalDateTime nowUtc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
		jdbcTemplate.update("UPDATE users SET last_seen_at = ? WHERE id = ? AND (last_seen_at IS NULL OR last_seen_at < ?)",
				nowUtc, userId, nowUtc.minus(LAST_SEEN_PRECISION));
	}
}
