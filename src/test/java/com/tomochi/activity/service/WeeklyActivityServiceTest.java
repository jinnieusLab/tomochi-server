package com.tomochi.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WeeklyActivityServiceTest {

	@ParameterizedTest
	@CsvSource({
			"2026-10-11, 2026-10-11", // 일요일은 그날
			"2026-10-14, 2026-10-11", // 수요일
			"2026-10-17, 2026-10-11", // 토요일
			"2026-10-18, 2026-10-18", // 다음 일요일
			"2027-01-01, 2026-12-27"  // 해를 넘김
	})
	void 주는_일요일에_시작한다(LocalDate date, LocalDate expectedSunday) {
		assertThat(WeeklyActivityService.weekStart(date)).isEqualTo(expectedSunday);
	}
}
