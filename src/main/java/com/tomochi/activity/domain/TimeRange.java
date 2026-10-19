package com.tomochi.activity.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * 반열린 구간 {@code [start, end)}. 끝과 시작이 딱 맞닿은 두 구간은 겹치지 않아요.
 */
public record TimeRange(Instant start, Instant end) {

	public TimeRange {
		if (!end.isAfter(start)) {
			throw new IllegalArgumentException("end는 start보다 뒤여야 해요: " + start + " ~ " + end);
		}
	}

	public Duration duration() {
		return Duration.between(start, end);
	}

	/** 두 구간의 겹친 부분. 겹치지 않으면 null. */
	public TimeRange intersect(Instant from, Instant to) {
		Instant s = start.isAfter(from) ? start : from;
		Instant e = end.isBefore(to) ? end : to;
		return e.isAfter(s) ? new TimeRange(s, e) : null;
	}
}
