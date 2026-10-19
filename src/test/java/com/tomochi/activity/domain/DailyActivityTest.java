package com.tomochi.activity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyActivityTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final ZoneId LA = ZoneId.of("America/Los_Angeles");
	private static final LocalDate DAY = LocalDate.of(2026, 10, 17);
	private static final Duration CAP = Duration.ofHours(20);

	@Test
	void 그날_구간의_합집합을_초로_센다() {
		DailyActivity day = DailyActivity.calculate(List.of(
				seoul("09:00", "10:00"), seoul("09:30", "11:00"), seoul("14:00", "14:30")), DAY, SEOUL, CAP);

		assertThat(day.seconds()).isEqualTo(Duration.ofMinutes(150).toSeconds());
	}

	@Test
	void 칸은_0시부터_30분_단위_48개이고_1초라도_걸치면_칠한다() {
		DailyActivity day = DailyActivity.calculate(List.of(seoul("09:10", "09:40"), seoul("23:59", "23:59:30")),
				DAY, SEOUL, CAP);

		assertThat(day.slots()).hasSize(48);
		assertThat(day.slots().charAt(18)).isEqualTo('1'); // 09:00~09:30
		assertThat(day.slots().charAt(19)).isEqualTo('1'); // 09:30~10:00
		assertThat(day.slots().charAt(20)).isEqualTo('0');
		assertThat(day.slots().charAt(47)).isEqualTo('1'); // 23:30~24:00
		assertThat(day.slots().chars().filter(c -> c == '1').count()).isEqualTo(3);
	}

	@Test
	void 칸_경계에서_끝나면_다음_칸은_칠하지_않는다() {
		DailyActivity day = DailyActivity.calculate(List.of(seoul("09:00", "09:30")), DAY, SEOUL, CAP);

		assertThat(day.slots().charAt(18)).isEqualTo('1');
		assertThat(day.slots().charAt(19)).isEqualTo('0');
	}

	@Test
	void 자정을_넘는_구간은_그날_몫만_센다() {
		TimeRange overMidnight = new TimeRange(
				DAY.atTime(23, 0).atZone(SEOUL).toInstant(), DAY.plusDays(1).atTime(1, 0).atZone(SEOUL).toInstant());

		assertThat(DailyActivity.calculate(List.of(overMidnight), DAY, SEOUL, CAP).seconds()).isEqualTo(3600);
		assertThat(DailyActivity.calculate(List.of(overMidnight), DAY.plusDays(1), SEOUL, CAP).seconds())
				.isEqualTo(3600);
	}

	@Test
	void 날짜는_사용자_시간대로_나눈다() {
		// UTC 10월 17일 05:00~06:00 = 서울 17일 14시, LA 16일 22시
		TimeRange range = new TimeRange(Instant.parse("2026-10-17T05:00:00Z"), Instant.parse("2026-10-17T06:00:00Z"));

		assertThat(DailyActivity.calculate(List.of(range), DAY, SEOUL, CAP).seconds()).isEqualTo(3600);
		assertThat(DailyActivity.calculate(List.of(range), DAY, LA, CAP).seconds()).isZero();
		assertThat(DailyActivity.calculate(List.of(range), DAY.minusDays(1), LA, CAP).seconds()).isEqualTo(3600);
	}

	@Test
	void 하루_상한을_넘으면_뒤에서부터_자른다() {
		DailyActivity day = DailyActivity.calculate(List.of(seoul("00:00", "11:00"), seoul("12:00", "23:30")),
				DAY, SEOUL, CAP);

		assertThat(day.seconds()).isEqualTo(CAP.toSeconds());
		// 11시간 + 9시간 = 20시간 → 21:00까지만 칠해요.
		assertThat(day.slots().charAt(41)).isEqualTo('1'); // 20:30~21:00
		assertThat(day.slots().charAt(42)).isEqualTo('0'); // 21:00~21:30
	}

	@Test
	void 구간이_없으면_0() {
		DailyActivity day = DailyActivity.calculate(List.of(), DAY, SEOUL, CAP);

		assertThat(day.seconds()).isZero();
		assertThat(day.slots()).isEqualTo("0".repeat(48));
	}

	private static TimeRange seoul(String start, String end) {
		return new TimeRange(DAY.atTime(java.time.LocalTime.parse(start)).atZone(SEOUL).toInstant(),
				DAY.atTime(java.time.LocalTime.parse(end)).atZone(SEOUL).toInstant());
	}
}
