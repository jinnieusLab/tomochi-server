package com.tomochi.user.batch;

import com.tomochi.user.domain.User;
import com.tomochi.user.service.UserPurger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 탈퇴하고 7일이 지난 계정을 지워요. 매일 새벽 4시 40분(KST).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserPurgeJob {

	private static final int BATCH_SIZE = 100;

	private final JdbcTemplate jdbcTemplate;
	private final UserPurger userPurger;
	private final Clock clock;

	@Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
	public void run() {
		LocalDateTime cutoff = LocalDateTime.ofInstant(clock.instant().minus(User.WITHDRAWAL_GRACE), ZoneOffset.UTC);
		int purged = 0;
		List<Long> ids;
		do {
			ids = jdbcTemplate.queryForList(
					"SELECT id FROM users WHERE deleted_at IS NOT NULL AND deleted_at <= ? ORDER BY id LIMIT " + BATCH_SIZE,
					Long.class, cutoff);
			for (Long id : ids) {
				userPurger.purge(id);
				purged++;
			}
		} while (ids.size() == BATCH_SIZE);
		log.info("탈퇴 계정 정리: {}명", purged);
	}
}
