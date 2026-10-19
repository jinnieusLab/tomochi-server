package com.tomochi.activity.batch;

import com.tomochi.activity.domain.SegmentPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 보관 기간(186일)이 지난 작업 구간과 하루 요약을 지워요. 매일 새벽 4시 30분(KST).
 *
 * <p>한 번에 지우면 큰 DELETE가 오래 잠금을 잡고 복제·언두 로그가 커져서, {@value #BATCH_SIZE}개씩 나눠 지워요.
 * 각 DELETE는 자기 트랜잭션이라 중간에 실패해도 다음 날 이어서 지우면 돼요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityRetentionJob {

	static final int BATCH_SIZE = 1_000;

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	@Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
	public void run() {
		Instant now = clock.instant();
		int segments = deleteSegmentsEndedBefore(now.minus(Duration.ofDays(SegmentPolicy.RETENTION_DAYS)));
		// 사용자 시간대마다 "오늘"이 달라서, 하루 여유를 두고 UTC 기준으로 지워요.
		int days = deleteDailyActivityBefore(LocalDate.ofInstant(now, ZoneOffset.UTC)
				.minusDays(SegmentPolicy.RETENTION_DAYS + 1L));
		int together = deleteInBatches("DELETE FROM together_daily WHERE date < ? LIMIT " + BATCH_SIZE,
				LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(SegmentPolicy.RETENTION_DAYS + 1L));
		log.info("오래된 활동 기록 정리: 구간 {}개, 하루 요약 {}개, 둘이 함께한 날 {}개", segments, days, together);
	}

	int deleteSegmentsEndedBefore(Instant cutoff) {
		return deleteInBatches("DELETE FROM activity_segment WHERE ended_at < ? LIMIT " + BATCH_SIZE,
				LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC));
	}

	int deleteDailyActivityBefore(LocalDate cutoff) {
		return deleteInBatches("DELETE FROM daily_activity WHERE date < ? LIMIT " + BATCH_SIZE, cutoff);
	}

	private int deleteInBatches(String sql, Object cutoff) {
		int total = 0;
		int deleted;
		do {
			deleted = jdbcTemplate.update(sql, cutoff);
			total += deleted;
		} while (deleted == BATCH_SIZE);
		return total;
	}
}
