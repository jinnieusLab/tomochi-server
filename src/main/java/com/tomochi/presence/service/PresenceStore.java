package com.tomochi.presence.service;

import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 접속 상태를 Redis에 둬요. 자주 바뀌고 잃어버려도 괜찮은 값이라 DB에 쓰지 않아요.
 *
 * <pre>
 * presence:{userId}  (해시)
 *   {sessionId} → "STATUS|ICON|workingSinceMillis|updatedAtMillis"   필드마다 TTL 90초 (HEXPIRE, Redis 7.4+)
 * </pre>
 * 기기(세션)마다 필드 하나라, 하트비트가 끊긴 기기만 저절로 사라져요. 앱이 비정상 종료돼도 90초 뒤 정리돼요.
 */
@Component
@RequiredArgsConstructor
public class PresenceStore {

	public static final Duration TTL = Duration.ofSeconds(90);
	private static final String KEY_PREFIX = "presence:";

	/**
	 * 이전 값을 읽어 workingSince를 정하고 쓰는 것까지 한 번에(원자적으로) 해요.
	 * 작업 중이 아니었다가 작업 중이 될 때만 workingSince를 지금으로 정하고, 계속 작업 중이면 그대로 둬요.
	 */
	private static final RedisScript<String> UPDATE = RedisScript.of("""
			local previous = redis.call('HGET', KEYS[1], ARGV[1])
			local status, now, ttl = ARGV[2], ARGV[4], ARGV[5]
			local since = ''
			if status == 'WORKING' then
			    since = now
			    if previous then
			        local prevStatus, _, prevSince = string.match(previous, '^([^|]*)|([^|]*)|([^|]*)|')
			        if prevStatus == 'WORKING' and prevSince ~= '' then
			            since = prevSince
			        end
			    end
			end
			redis.call('HSET', KEYS[1], ARGV[1], status .. '|' .. ARGV[3] .. '|' .. since .. '|' .. now)
			redis.call('HEXPIRE', KEYS[1], ttl, 'FIELDS', 1, ARGV[1])
			redis.call('EXPIRE', KEYS[1], ttl)
			return since
			""", String.class);

	/**
	 * 모든 기기의 아이콘만 바꿔요. 해시 필드를 덮어쓰면 그 필드의 TTL이 지워져서, 남은 TTL을 읽어 다시 걸어요.
	 */
	private static final RedisScript<Long> CHANGE_ICON = RedisScript.of("""
			local fields = redis.call('HGETALL', KEYS[1])
			local changed = 0
			for i = 1, #fields, 2 do
			    local field, value = fields[i], fields[i + 1]
			    local status, _, rest = string.match(value, '^([^|]*)|([^|]*)|(.*)$')
			    local ttl = redis.call('HTTL', KEYS[1], 'FIELDS', 1, field)[1]
			    if status and ttl > 0 then
			        redis.call('HSET', KEYS[1], field, status .. '|' .. ARGV[1] .. '|' .. rest)
			        redis.call('HEXPIRE', KEYS[1], ttl, 'FIELDS', 1, field)
			        changed = changed + 1
			    end
			end
			return changed
			""", Long.class);

	private final StringRedisTemplate redisTemplate;

	/** @return 이 기기의 workingSince (작업 중이 아니면 null) */
	public Instant update(Long userId, String sessionId, PresenceStatus status, StatusIcon icon, Instant now) {
		String since = redisTemplate.execute(UPDATE, List.of(KEY_PREFIX + userId),
				sessionId, status.name(), icon != null ? icon.name() : "", String.valueOf(now.toEpochMilli()),
				String.valueOf(TTL.toSeconds()));
		return since == null || since.isEmpty() ? null : Instant.ofEpochMilli(Long.parseLong(since));
	}

	/** 지금 접속 중인 기기들의 아이콘을 바로 바꿔요. 다음 하트비트를 기다리지 않고 친구에게 보여요. */
	public void changeIcon(Long userId, StatusIcon icon) {
		redisTemplate.execute(CHANGE_ICON, List.of(KEY_PREFIX + userId), icon != null ? icon.name() : "");
	}

	/** 여러 사람의 상태를 한 번에(pipeline) 읽어요. 친구 목록을 보여줄 때 친구 수만큼 왕복하지 않게요. */
	public Map<Long, Presence> findAll(List<Long> userIds, Instant now) {
		// 결과는 템플릿이 문자열로 바꿔 돌려줘요 (Map<String, String>).
		List<Object> replies = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
			for (Long userId : userIds) {
				connection.hashCommands().hGetAll((KEY_PREFIX + userId).getBytes(StandardCharsets.UTF_8));
			}
			return null;
		});
		Map<Long, Presence> presences = new HashMap<>();
		for (int i = 0; i < userIds.size(); i++) {
			@SuppressWarnings("unchecked")
			Map<String, String> fields = (Map<String, String>) replies.get(i);
			presences.put(userIds.get(i), Presence.merge(devices(fields, now)));
		}
		return presences;
	}

	public Presence find(Long userId, Instant now) {
		return findAll(List.of(userId), now).get(userId);
	}

	private static List<Presence.Device> devices(Map<String, String> fields, Instant now) {
		List<Presence.Device> devices = new ArrayList<>();
		if (fields == null) {
			return devices;
		}
		for (String value : fields.values()) {
			String[] parts = value.split("\\|", -1);
			Instant updatedAt = Instant.ofEpochMilli(Long.parseLong(parts[3]));
			// HEXPIRE가 지우기 직전의 필드도 거르려고 한 번 더 봐요.
			if (updatedAt.plus(TTL).isBefore(now)) {
				continue;
			}
			devices.add(new Presence.Device(
					PresenceStatus.valueOf(parts[0]),
					parts[1].isEmpty() ? null : StatusIcon.valueOf(parts[1]),
					parts[2].isEmpty() ? null : Instant.ofEpochMilli(Long.parseLong(parts[2])),
					updatedAt));
		}
		return devices;
	}
}
