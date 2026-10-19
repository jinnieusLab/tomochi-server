package com.tomochi.together.domain;

import com.tomochi.activity.domain.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * 겹친 구간을 한 사람의 달력(시간대)으로 날짜별로 나눠요. 두 사람의 시간대가 다르면 "그날"이 서로 달라서,
 * 같은 겹침을 각자의 시간대로 한 번씩 나눠 따로 저장해요.
 */
public final class TogetherDays {

	private TogetherDays() {
	}

	/** @return 날짜 → 그날 겹친 초 (날짜 순) */
	public static Map<LocalDate, Long> split(Collection<TimeRange> overlaps, ZoneId zone) {
		Map<LocalDate, Long> seconds = new TreeMap<>();
		for (TimeRange overlap : overlaps) {
			LocalDate date = LocalDate.ofInstant(overlap.start(), zone);
			Instant cursor = overlap.start();
			while (cursor.isBefore(overlap.end())) {
				Instant nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant();
				Instant end = overlap.end().isBefore(nextMidnight) ? overlap.end() : nextMidnight;
				seconds.merge(date, Duration.between(cursor, end).toSeconds(), Long::sum);
				cursor = end;
				date = date.plusDays(1);
			}
		}
		seconds.values().removeIf(value -> value == 0);
		return seconds;
	}
}
