package com.tomochi.activity.repository;

import com.tomochi.activity.domain.DailyActivity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 지난 날 하루 요약 캐시. 30분 칸 48개는 비트(BIGINT)로 저장해요.
 */
@Repository
@RequiredArgsConstructor
public class DailyActivityCache {

	private final JdbcTemplate jdbcTemplate;

	/** 지금 시간대로 계산해 둔 날만 돌려줘요. 시간대가 바뀌기 전에 계산한 값은 버려요. */
	public Map<LocalDate, DailyActivity> find(Long userId, LocalDate from, LocalDate to, String timezone) {
		Map<LocalDate, DailyActivity> cached = new HashMap<>();
		jdbcTemplate.query("""
						SELECT date, seconds, slots FROM daily_activity
						WHERE user_id = ? AND date BETWEEN ? AND ? AND timezone = ?
						""",
				rs -> {
					LocalDate date = rs.getObject(1, LocalDate.class);
					cached.put(date, new DailyActivity(date, rs.getLong(2), toSlots(rs.getLong(3))));
				},
				userId, from, to, timezone);
		return cached;
	}

	/** 두 요청이 같은 날을 동시에 계산해도 같은 값이라 덮어써도 돼요. */
	public void save(Long userId, List<DailyActivity> days, String timezone, Instant now) {
		LocalDateTime computedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
		jdbcTemplate.batchUpdate("""
						INSERT INTO daily_activity (user_id, date, timezone, seconds, slots, computed_at)
						VALUES (?, ?, ?, ?, ?, ?) AS incoming
						ON DUPLICATE KEY UPDATE
						    timezone = incoming.timezone, seconds = incoming.seconds,
						    slots = incoming.slots, computed_at = incoming.computed_at
						""",
				days.stream()
						.map(day -> new Object[] {userId, day.date(), timezone, day.seconds(), toBits(day.slots()), computedAt})
						.toList());
	}

	static long toBits(String slots) {
		long bits = 0;
		for (int i = 0; i < slots.length(); i++) {
			if (slots.charAt(i) == '1') {
				bits |= 1L << i;
			}
		}
		return bits;
	}

	static String toSlots(long bits) {
		StringBuilder slots = new StringBuilder(DailyActivity.SLOT_COUNT);
		for (int i = 0; i < DailyActivity.SLOT_COUNT; i++) {
			slots.append((bits & (1L << i)) != 0 ? '1' : '0');
		}
		return slots.toString();
	}
}
