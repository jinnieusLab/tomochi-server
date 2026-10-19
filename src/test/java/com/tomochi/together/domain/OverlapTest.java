package com.tomochi.together.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.activity.domain.TimeRange;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * <pre>
 * A: |■■■■■■■■|      |■■■■■■|
 * B:      |■■■■■■■■■■■■■|
 * 겹침:    |■■■|      |■■■|
 * </pre>
 */
class OverlapTest {

	@Test
	void 안_겹치면_없다() {
		assertThat(Overlap.between(List.of(r("09:00", "10:00")), List.of(r("11:00", "12:00")))).isEmpty();
	}

	@Test
	void 한_구간이_다른_구간을_품으면_작은_쪽이_겹침이다() {
		assertThat(Overlap.between(List.of(r("09:00", "12:00")), List.of(r("10:00", "11:00"))))
				.containsExactly(r("10:00", "11:00"));
	}

	@Test
	void 끝과_시작이_딱_맞닿으면_겹침은_0이다() {
		assertThat(Overlap.between(List.of(r("09:00", "10:00")), List.of(r("10:00", "11:00")))).isEmpty();
	}

	@Test
	void 여러_구간이_엇갈려도_겹친_부분만_모은다() {
		List<TimeRange> a = List.of(r("09:00", "11:00"), r("13:00", "15:00"));
		List<TimeRange> b = List.of(r("10:00", "14:00"));

		assertThat(Overlap.between(a, b)).containsExactly(r("10:00", "11:00"), r("13:00", "14:00"));
	}

	@Test
	void 한쪽_기기_여러_대가_겹쳐도_두_번_세지_않는다() {
		// A가 맥북·아이맥을 같이 켜 둠
		List<TimeRange> a = List.of(r("09:00", "10:00"), r("09:30", "11:00"));
		List<TimeRange> b = List.of(r("09:00", "12:00"));

		assertThat(Overlap.between(a, b)).containsExactly(r("09:00", "11:00"));
	}

	@Test
	void 정렬되지_않은_구간도_된다() {
		List<TimeRange> a = List.of(r("13:00", "15:00"), r("09:00", "11:00"));
		List<TimeRange> b = List.of(r("14:00", "16:00"), r("08:00", "09:30"));

		assertThat(Overlap.between(a, b)).containsExactly(r("09:00", "09:30"), r("14:00", "15:00"));
	}

	@Test
	void 한쪽이_비면_없다() {
		assertThat(Overlap.between(List.of(), List.of(r("09:00", "10:00")))).isEmpty();
	}

	static TimeRange r(String start, String end) {
		return new TimeRange(Instant.parse("2026-10-17T" + start + ":00Z"), Instant.parse("2026-10-17T" + end + ":00Z"));
	}
}
