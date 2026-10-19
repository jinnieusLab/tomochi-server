package com.tomochi.activity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SegmentPolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
	private static final String ID = "8f6c2a1e-4b7d-4c3a-9f10-2d5e6a7b8c9d";

	@Test
	void 올바른_구간은_그대로_받는다() {
		SegmentPolicy.Result result = check(NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(1)));

		assertThat(result.isAccepted()).isTrue();
		assertThat(result.adjusted()).isFalse();
		assertThat(result.range().duration()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void 끝이_시작보다_뒤가_아니면_거절한다() {
		assertThat(check(NOW.minusSeconds(60), NOW.minusSeconds(60)).rejectReason()).isEqualTo(RejectReason.INVALID_RANGE);
		assertThat(check(NOW.minusSeconds(60), NOW.minusSeconds(120)).rejectReason()).isEqualTo(RejectReason.INVALID_RANGE);
		assertThat(check(null, NOW).rejectReason()).isEqualTo(RejectReason.INVALID_RANGE);
	}

	@Test
	void 끝이_2분_넘게_미래면_서버_시각으로_자른다() {
		SegmentPolicy.Result result = check(NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofMinutes(10)));

		assertThat(result.isAccepted()).isTrue();
		assertThat(result.adjusted()).isTrue();
		assertThat(result.range().end()).isEqualTo(NOW);
	}

	@Test
	void 시계_차이_2분까지는_그대로_둔다() {
		SegmentPolicy.Result result = check(NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofMinutes(2)));

		assertThat(result.adjusted()).isFalse();
		assertThat(result.range().end()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
	}

	@Test
	void 시작부터_미래면_거절한다() {
		assertThat(check(NOW.plus(Duration.ofMinutes(5)), NOW.plus(Duration.ofMinutes(10))).rejectReason())
				.isEqualTo(RejectReason.IN_FUTURE);
	}

	@Test
	void 칠일보다_오래되면_거절한다() {
		assertThat(check(NOW.minus(Duration.ofDays(7)).minusSeconds(1), NOW.minus(Duration.ofDays(6))).rejectReason())
				.isEqualTo(RejectReason.TOO_OLD);
		assertThat(check(NOW.minus(Duration.ofDays(7)), NOW.minus(Duration.ofDays(7)).plusSeconds(60)).isAccepted())
				.isTrue();
	}

	@Test
	void 열두_시간보다_길면_거절한다() {
		Instant start = NOW.minus(Duration.ofHours(13));
		assertThat(check(start, start.plus(Duration.ofHours(12))).isAccepted()).isTrue();
		assertThat(check(start, start.plus(Duration.ofHours(12)).plusSeconds(1)).rejectReason())
				.isEqualTo(RejectReason.TOO_LONG);
	}

	@Test
	void id가_UUID가_아니면_거절한다() {
		assertThat(SegmentPolicy.check("not-a-uuid", NOW.minusSeconds(120), NOW.minusSeconds(60), NOW).rejectReason())
				.isEqualTo(RejectReason.INVALID_ID);
		assertThat(SegmentPolicy.check(null, NOW.minusSeconds(120), NOW.minusSeconds(60), NOW).rejectReason())
				.isEqualTo(RejectReason.INVALID_ID);
	}

	@Test
	void 비ASCII_숫자가_섞인_id는_거절한다() {
		// 끝자리 '９'는 전각 숫자예요.
		String fullWidth = "8f6c2a1e-4b7d-4c3a-9f10-2d5e6a7b8c9\uFF19";

		assertThat(SegmentPolicy.check(fullWidth, NOW.minusSeconds(120), NOW.minusSeconds(60), NOW).rejectReason())
				.isEqualTo(RejectReason.INVALID_ID);
	}

	@Test
	void 대문자_id는_소문자로_맞춘다() {
		SegmentPolicy.Result result =
				SegmentPolicy.check(ID.toUpperCase(), NOW.minusSeconds(120), NOW.minusSeconds(60), NOW);

		assertThat(result.clientSegmentId()).isEqualTo(ID);
	}

	@Test
	void 마이크로초_아래는_버린다() {
		Instant start = Instant.parse("2026-10-17T10:00:00.123456789Z");
		SegmentPolicy.Result result = check(start, start.plusSeconds(60));

		assertThat(result.range().start()).isEqualTo(Instant.parse("2026-10-17T10:00:00.123456Z"));
	}

	private static SegmentPolicy.Result check(Instant start, Instant end) {
		return SegmentPolicy.check(ID, start, end, NOW);
	}
}
