package com.tomochi.activity.repository;

import com.tomochi.activity.domain.TimeRange;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 구간 저장. 같은 {@code (user_id, client_segment_id)}가 이미 있으면 범위를 넓히기만 해요.
 *
 * <p>네트워크가 끊겨 같은 요청을 다시 보내도, 앱이 열린 구간을 "지금까지"로 늘려 다시 보내도 시간이 두 번 더해지지 않아요.
 * {@code LEAST}·{@code GREATEST}는 순서와 상관없이 같은 결과라, 같은 id가 동시에 와도 잠금 없이 합집합이 남아요.
 * InnoDB가 중복 키 행을 잠그고 차례로 UPDATE해요.
 */
@Repository
@RequiredArgsConstructor
public class ActivitySegmentWriter {

	private static final String UPSERT = """
			INSERT INTO activity_segment (user_id, client_segment_id, started_at, ended_at, created_at, updated_at)
			VALUES (?, ?, ?, ?, ?, ?) AS incoming
			ON DUPLICATE KEY UPDATE
			    started_at = LEAST(activity_segment.started_at, incoming.started_at),
			    ended_at   = GREATEST(activity_segment.ended_at, incoming.ended_at),
			    updated_at = incoming.updated_at
			""";

	private final JdbcTemplate jdbcTemplate;

	/** @param segments clientSegmentId → 구간. 한 요청 안의 같은 id는 미리 합쳐서 넘겨요 */
	public void upsert(Long userId, Map<String, TimeRange> segments, Instant now) {
		LocalDateTime nowUtc = utc(now);
		// 두 요청이 같은 id들을 서로 다른 순서로 잠그면 데드락이 날 수 있어서, 늘 id 순서로 저장해요.
		List<Object[]> rows = segments.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new Object[] {
						userId,
						entry.getKey(),
						utc(entry.getValue().start()),
						utc(entry.getValue().end()),
						nowUtc,
						nowUtc})
				.toList();
		jdbcTemplate.batchUpdate(UPSERT, rows);
	}

	/**
	 * DATETIME(6)은 시간대가 없어서 UTC 벽시계 값으로 넣어요. {@code java.sql.Timestamp}를 넘기면 드라이버가
	 * JVM 시간대(예: 서울)의 벽시계로 바꿔 저장해서, 같은 JVM에서 읽을 때만 맞고 저장 값은 9시간 어긋나요.
	 */
	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
