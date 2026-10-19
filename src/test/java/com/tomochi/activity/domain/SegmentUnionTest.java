package com.tomochi.activity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SegmentUnionTest {

	@Test
	void 겹치지_않으면_그대로_정렬만_한다() {
		assertThat(SegmentUnion.of(List.of(range("11:00", "12:00"), range("09:00", "10:00"))))
				.containsExactly(range("09:00", "10:00"), range("11:00", "12:00"));
	}

	@Test
	void 겹치면_하나로_합친다() {
		assertThat(SegmentUnion.of(List.of(range("09:00", "10:00"), range("09:30", "11:00"))))
				.containsExactly(range("09:00", "11:00"));
	}

	@Test
	void 한_구간이_다른_구간을_품으면_큰_쪽만_남는다() {
		assertThat(SegmentUnion.of(List.of(range("09:00", "12:00"), range("10:00", "11:00"))))
				.containsExactly(range("09:00", "12:00"));
	}

	@Test
	void 끝과_시작이_맞닿으면_이어_붙인다() {
		assertThat(SegmentUnion.of(List.of(range("09:00", "10:00"), range("10:00", "11:00"))))
				.containsExactly(range("09:00", "11:00"));
	}

	@Test
	void 여러_기기의_구간이_엇갈려도_합집합이다() {
		List<TimeRange> union = SegmentUnion.of(List.of(
				range("09:00", "10:00"), range("13:00", "14:00"),
				range("09:30", "10:30"), range("10:20", "11:00"),
				range("13:30", "13:40")));

		assertThat(union).containsExactly(range("09:00", "11:00"), range("13:00", "14:00"));
	}

	@Test
	void 같은_구간이_여러_번_와도_한_번만_센다() {
		assertThat(SegmentUnion.of(List.of(range("09:00", "10:00"), range("09:00", "10:00"))))
				.containsExactly(range("09:00", "10:00"));
	}

	@Test
	void 비어_있으면_비어_있다() {
		assertThat(SegmentUnion.of(List.of())).isEmpty();
	}

	static TimeRange range(String start, String end) {
		return new TimeRange(Instant.parse("2026-10-17T" + start + ":00Z"), Instant.parse("2026-10-17T" + end + ":00Z"));
	}
}
