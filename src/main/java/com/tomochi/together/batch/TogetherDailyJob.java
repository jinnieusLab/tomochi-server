package com.tomochi.together.batch;

import com.tomochi.together.service.TogetherAggregator;
import com.tomochi.together.service.TogetherAggregator.Member;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 둘이 함께한 시간 일 집계. 매일 새벽 4시(KST)에 친구 쌍마다 <b>최근 9일을 통째로 다시 계산</b>해요.
 *
 * <p>업로드는 7일 전 구간까지 늦게 들어올 수 있어요. "다시 집계할 날" 표시 테이블을 두는 대신, 늦은 구간이 걸칠 수 있는
 * 기간 전체를 매번 덮어써요(멱등). 친구는 20명까지라 쌍마다 9일 치 구간을 읽는 비용은 작아요.
 * 실패해도 다음 날 다시 돌리면 같은 결과가 돼요. 오늘처럼 아직 안 끝난 날은 조회할 때 즉석으로 계산해요.
 */
@Slf4j
@Component
public class TogetherDailyJob {

	static final Duration WINDOW = Duration.ofDays(9);
	private static final int PAGE_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;
	private final TogetherAggregator aggregator;
	private final Clock clock;
	private final Timer timer;

	public TogetherDailyJob(JdbcTemplate jdbcTemplate, TogetherAggregator aggregator, Clock clock,
			MeterRegistry meterRegistry) {
		this.jdbcTemplate = jdbcTemplate;
		this.aggregator = aggregator;
		this.clock = clock;
		this.timer = Timer.builder("tomochi.together.batch").description("둘이 함께한 시간 일 집계 소요 시간").register(meterRegistry);
	}

	@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
	public void run() {
		timer.record(this::aggregateAll);
	}

	void aggregateAll() {
		Instant to = clock.instant();
		Instant from = to.minus(WINDOW);
		long lastId = 0;
		int pairs = 0;
		int rows = 0;
		List<Pair> page;
		do {
			page = jdbcTemplate.query("""
							SELECT f.id, a.id, a.timezone, b.id, b.timezone
							FROM friendship f
							JOIN users a ON a.id = f.user_low AND a.deleted_at IS NULL
							JOIN users b ON b.id = f.user_high AND b.deleted_at IS NULL
							WHERE f.id > ? ORDER BY f.id LIMIT ?
							""",
					(rs, n) -> new Pair(rs.getLong(1),
							new Member(rs.getLong(2), ZoneId.of(rs.getString(3))),
							new Member(rs.getLong(4), ZoneId.of(rs.getString(5)))),
					lastId, PAGE_SIZE);
			for (Pair pair : page) {
				rows += aggregator.aggregate(pair.a(), pair.b(), from, to);
				pairs++;
				lastId = pair.id();
			}
		} while (page.size() == PAGE_SIZE);
		log.info("둘이 함께한 시간 집계: 친구 쌍 {}개, 저장한 날 {}줄", pairs, rows);
	}

	private record Pair(long id, Member a, Member b) {
	}
}
