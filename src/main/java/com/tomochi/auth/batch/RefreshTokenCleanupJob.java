package com.tomochi.auth.batch;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료되거나 폐기된 지 7일이 지난 Refresh Token을 지워요. 매일 새벽 4시 50분(KST).
 *
 * <p>폐기된 토큰을 바로 지우지 않는 이유: 재사용 감지(AUTH-2)는 "폐기된 토큰이 다시 왔다"를 알아야 해요.
 * 지워 버리면 훔친 토큰이 와도 "없는 토큰"으로만 보여서 그 기기를 끊을 수 없어요. 7일이면 충분히 지켜봐요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

	static final Duration KEEP_AFTER_END = Duration.ofDays(7);
	private static final int BATCH_SIZE = 1_000;

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	@Scheduled(cron = "0 50 4 * * *", zone = "Asia/Seoul")
	public void run() {
		LocalDateTime cutoff = LocalDateTime.ofInstant(clock.instant().minus(KEEP_AFTER_END), ZoneOffset.UTC);
		int total = 0;
		int deleted;
		do {
			deleted = jdbcTemplate.update(
					"DELETE FROM refresh_token WHERE expires_at < ? OR revoked_at < ? LIMIT " + BATCH_SIZE, cutoff, cutoff);
			total += deleted;
		} while (deleted == BATCH_SIZE);
		log.info("지난 Refresh Token 정리: {}개", total);
	}
}
