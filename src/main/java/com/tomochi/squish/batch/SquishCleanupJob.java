package com.tomochi.squish.batch;

import com.tomochi.squish.service.PendingSquishService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 보낸 지 30일이 지난 꾹을 지워요 (받았든 안 받았든). 매일 새벽 4시 35분(KST). */
@Slf4j
@Component
@RequiredArgsConstructor
public class SquishCleanupJob {

	private static final int BATCH_SIZE = 1_000;

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	@Scheduled(cron = "0 35 4 * * *", zone = "Asia/Seoul")
	public void run() {
		LocalDateTime cutoff = LocalDateTime.ofInstant(clock.instant().minus(PendingSquishService.KEEP), ZoneOffset.UTC);
		int total = 0;
		int deleted;
		do {
			deleted = jdbcTemplate.update("DELETE FROM squish WHERE sent_at < ? LIMIT " + BATCH_SIZE, cutoff);
			total += deleted;
		} while (deleted == BATCH_SIZE);
		log.info("지난 꾹 정리: {}개", total);
	}
}
