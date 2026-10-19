package com.tomochi.global.ratelimit;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 고정 창 카운터. 창마다 키 하나를 두고 INCR해요.
 * INCR과 첫 요청의 만료 설정을 Lua 스크립트 하나로 묶어서, 동시에 와도 만료 없는 키가 남지 않아요.
 * 서버가 여러 대여도 같은 Redis를 보니 함께 세요.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

	private static final RedisScript<List> INCREMENT = RedisScript.of("""
			local count = redis.call('INCR', KEYS[1])
			if count == 1 then
			    redis.call('PEXPIRE', KEYS[1], ARGV[1])
			end
			return {count, redis.call('PTTL', KEYS[1])}
			""", List.class);

	private final StringRedisTemplate redisTemplate;

	/**
	 * @param key   {@code rl:{scope}:{userId 또는 ip}}
	 * @return 허용이면 {@link Result#allowed()}, 아니면 창이 끝날 때까지 남은 초
	 */
	@SuppressWarnings("unchecked")
	public Result acquire(String key, int limit, Duration window) {
		List<Long> reply = redisTemplate.execute(INCREMENT, List.of(key), String.valueOf(window.toMillis()));
		long count = reply.get(0);
		long ttlMillis = Math.max(reply.get(1), 0);
		if (count <= limit) {
			return Result.ALLOWED;
		}
		return new Result(false, Math.max(1, (ttlMillis + 999) / 1000));
	}

	public record Result(boolean allowed, long retryAfterSeconds) {
		static final Result ALLOWED = new Result(true, 0);
	}
}
