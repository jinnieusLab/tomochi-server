package com.tomochi.activity.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 앱이 보낸 구간 하나를 검증해요. 서버가 시간을 직접 계산하려고 합계가 아니라 구간(시작·끝)을 받아요.
 *
 * <ul>
 *   <li>끝 ≤ 시작이면 거절</li>
 *   <li>끝이 서버 시각 + 2분보다 뒤면 서버 시각으로 잘라요 (시계 차이는 2분까지 봐줘요)</li>
 *   <li>시작이 7일보다 전이면 거절 (오프라인으로 쌓아 둘 수 있는 범위)</li>
 *   <li>12시간보다 길면 거절</li>
 * </ul>
 * 완벽한 부정 방지는 목표가 아니에요. 입력 내용을 보지 않으니 "가짜 작업"은 구분할 수 없고,
 * 랭킹이 없어서 부풀릴 동기도 거의 없어요. 숫자가 망가지지 않게만 막아요.
 */
public final class SegmentPolicy {

	public static final Duration CLOCK_SKEW = Duration.ofMinutes(2);
	public static final Duration MAX_AGE = Duration.ofDays(7);
	public static final Duration MAX_LENGTH = Duration.ofHours(12);
	public static final Duration DAILY_CAP = Duration.ofHours(20);
	public static final int MAX_SEGMENTS_PER_REQUEST = 200;

	private SegmentPolicy() {
	}

	public static Result check(String clientSegmentId, Instant start, Instant end, Instant now) {
		if (!isUuid(clientSegmentId)) {
			return Result.rejected(RejectReason.INVALID_ID);
		}
		if (start == null || end == null || !end.isAfter(start)) {
			return Result.rejected(RejectReason.INVALID_RANGE);
		}
		// DB는 마이크로초까지 저장해서 미리 맞춰 둬요.
		start = start.truncatedTo(ChronoUnit.MICROS);
		end = end.truncatedTo(ChronoUnit.MICROS);
		Instant serverNow = now.truncatedTo(ChronoUnit.MICROS);

		boolean adjusted = false;
		if (end.isAfter(serverNow.plus(CLOCK_SKEW))) {
			end = serverNow;
			adjusted = true;
		}
		if (!end.isAfter(start)) {
			return Result.rejected(RejectReason.IN_FUTURE);
		}
		if (start.isBefore(serverNow.minus(MAX_AGE))) {
			return Result.rejected(RejectReason.TOO_OLD);
		}
		if (Duration.between(start, end).compareTo(MAX_LENGTH) > 0) {
			return Result.rejected(RejectReason.TOO_LONG);
		}
		return new Result(new TimeRange(start, end), adjusted, null);
	}

	private static boolean isUuid(String value) {
		if (value == null || value.length() != 36) {
			return false;
		}
		try {
			UUID.fromString(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * @param range    저장할 구간 (거절이면 null)
	 * @param adjusted 미래 시각을 잘랐는지
	 */
	public record Result(TimeRange range, boolean adjusted, RejectReason rejectReason) {

		static Result rejected(RejectReason reason) {
			return new Result(null, false, reason);
		}

		public boolean isAccepted() {
			return rejectReason == null;
		}
	}
}
