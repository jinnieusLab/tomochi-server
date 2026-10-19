package com.tomochi.squish.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 같은 친구에게 10초에 한 번. 확인("대기 중인가?")과 기록("이제 대기 시작")을 따로 하면 동시에 누른 두 요청이 모두 통과해요.
 * {@code SET squish:cd:{from}:{to} 1 NX EX 10} 명령 하나로 "없으면 만들고 10초 뒤 사라지기"를 원자적으로 해서,
 * 키를 만든 요청 하나만 통과해요. 서버가 여러 대여도 같은 Redis라 그대로 동작해요.
 */
@Component
@RequiredArgsConstructor
public class SquishCooldown {

	public static final Duration COOLDOWN = Duration.ofSeconds(10);

	private final StringRedisTemplate redisTemplate;

	/** @return 지금 보낼 수 있으면 true (그리고 10초 대기 시작) */
	public boolean tryStart(Long fromUserId, Long toUserId) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key(fromUserId, toUserId), "1", COOLDOWN));
	}

	/** 남은 대기 초. 최소 1초 */
	public long remainingSeconds(Long fromUserId, Long toUserId) {
		Long ttl = redisTemplate.getExpire(key(fromUserId, toUserId));
		return ttl == null || ttl < 1 ? 1 : ttl;
	}

	private static String key(Long fromUserId, Long toUserId) {
		return "squish:cd:" + fromUserId + ":" + toUserId;
	}
}
