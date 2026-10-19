package com.tomochi.activity.repository;

import com.tomochi.activity.domain.SegmentPolicy;
import com.tomochi.activity.domain.TimeRange;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ActivitySegmentReader {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * {@code [from, to)}에 걸친 구간. {@code started_at}에 아래쪽 경계(from - 최대 길이 12시간)를 둬서
	 * {@code (user_id, started_at)} 인덱스로 범위를 좁혀요. 구간은 12시간을 넘지 않으니 빠지는 구간이 없어요.
	 */
	public List<TimeRange> findOverlapping(Long userId, Instant from, Instant to) {
		return jdbcTemplate.query("""
						SELECT started_at, ended_at FROM activity_segment
						WHERE user_id = ? AND started_at >= ? AND started_at < ? AND ended_at > ?
						""",
				(rs, rowNum) -> new TimeRange(
						rs.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC),
						rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
				userId, utc(from.minus(SegmentPolicy.MAX_LENGTH)), utc(to), utc(from));
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
