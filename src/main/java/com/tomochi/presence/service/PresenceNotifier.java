package com.tomochi.presence.service;

import com.tomochi.friend.repository.FriendQuery;
import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.user.domain.UserRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.tomochi.realtime.UserMessageSender;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 친구에게 내 상태 변화를 알려요 ({@code presence.changed}). 폴링(30초) 대신 바뀌는 순간 보내서,
 * 친구 목록이 바로 갱신되고 팝오버가 닫혀 있어도 요청이 오가지 않아요. 폴링은 연결이 끊겼을 때 대비로만 남겨요.
 *
 * <p>작업 중이 아니었다가 작업 중이 되면 {@code friend.workStarted}도 보내요 (친구 펫이 내 화면에 놀러 오는 신호).
 * 등장 규칙(1시간마다, 최대 2마리, 집중 모드·화면 공유 중 숨기기)은 앱이 정하고, 서버는 "시작했다"만 알려요.
 */
@Component
@RequiredArgsConstructor
public class PresenceNotifier {

	/** 쉬는 중 ↔ 작업 중을 짧게 오가도 같은 친구의 작업 시작은 15분에 한 번만 알려요. */
	static final Duration WORK_STARTED_THROTTLE = Duration.ofMinutes(15);

	private final FriendQuery friendQuery;
	private final UserMessageSender sender;
	private final UserRepository userRepository;
	private final StringRedisTemplate redisTemplate;

	/** 친구에게 보이는 값(상태·아이콘·작업 시작)이 바뀌었을 때만 보내요. 30초마다의 같은 하트비트로는 보내지 않아요. */
	public void notifyIfChanged(Long userId, Presence before, Presence after) {
		if (before.equals(after)) {
			return;
		}
		Map<String, Object> data = new HashMap<>();
		data.put("friendId", userId);
		data.put("status", after.status());
		data.put("icon", after.icon());
		data.put("workingSince", after.workingSince() != null ? after.workingSince().toString() : null);
		List<Long> watchers = friendQuery.findWatcherIds(userId);
		for (Long watcher : watchers) {
			sender.send(watcher, "presence.changed", data);
		}
		if (startedWorking(before, after) && claimWorkStarted(userId)) {
			notifyWorkStarted(userId, watchers);
		}
	}

	private static boolean startedWorking(Presence before, Presence after) {
		return before.status() != PresenceStatus.WORKING && after.status() == PresenceStatus.WORKING;
	}

	/** {@code SET friend:workStarted:{userId} 1 NX EX 900}: 15분 안에 처음일 때만 true (원자적) */
	private boolean claimWorkStarted(Long userId) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue()
				.setIfAbsent("friend:workStarted:" + userId, "1", WORK_STARTED_THROTTLE));
	}

	private void notifyWorkStarted(Long userId, List<Long> watchers) {
		userRepository.findById(userId).ifPresent(user -> {
			Map<String, Object> data = Map.of("friendId", userId, "petKind", user.getPetKind());
			for (Long watcher : watchers) {
				sender.send(watcher, "friend.workStarted", data);
			}
		});
	}
}
