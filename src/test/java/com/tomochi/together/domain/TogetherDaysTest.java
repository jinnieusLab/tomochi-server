package com.tomochi.together.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.activity.domain.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TogetherDaysTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

	@Test
	void 자정을_넘는_겹침은_두_날짜로_나뉜다() {
		// 서울 10/17 23:30 ~ 10/18 00:45
		TimeRange overlap = new TimeRange(Instant.parse("2026-10-17T14:30:00Z"), Instant.parse("2026-10-17T15:45:00Z"));

		Map<LocalDate, Long> days = TogetherDays.split(List.of(overlap), SEOUL);

		assertThat(days).containsExactly(
				Map.entry(LocalDate.of(2026, 10, 17), 1800L),
				Map.entry(LocalDate.of(2026, 10, 18), 2700L));
	}

	@Test
	void 시간대가_다른_두_사람은_같은_겹침을_다른_날짜로_본다() {
		// UTC 10/17 05:00~06:00 = 서울 10/17 14시, LA 10/16 22시
		TimeRange overlap = new TimeRange(Instant.parse("2026-10-17T05:00:00Z"), Instant.parse("2026-10-17T06:00:00Z"));

		assertThat(TogetherDays.split(List.of(overlap), SEOUL)).containsExactly(Map.entry(LocalDate.of(2026, 10, 17), 3600L));
		assertThat(TogetherDays.split(List.of(overlap), LA)).containsExactly(Map.entry(LocalDate.of(2026, 10, 16), 3600L));
	}

	@Test
	void 같은_날의_여러_겹침은_더한다() {
		List<TimeRange> overlaps = List.of(
				new TimeRange(Instant.parse("2026-10-17T01:00:00Z"), Instant.parse("2026-10-17T02:00:00Z")),
				new TimeRange(Instant.parse("2026-10-17T03:00:00Z"), Instant.parse("2026-10-17T03:30:00Z")));

		assertThat(TogetherDays.split(overlaps, SEOUL)).containsExactly(Map.entry(LocalDate.of(2026, 10, 17), 5400L));
	}
}
