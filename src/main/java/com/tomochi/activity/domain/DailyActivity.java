package com.tomochi.activity.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 사용자 시간대 기준 하루의 "함께한 시간"과 30분 칸 48개.
 *
 * @param seconds 그날 작업 구간 합집합의 길이. 하루 상한을 넘으면 상한까지만
 * @param slots   0시부터 30분 단위 48칸. 그 30분에 1초라도 작업이 걸치면 '1'
 */
public record DailyActivity(LocalDate date, long seconds, String slots) {

	public static final int SLOT_COUNT = 48;
	public static final Duration SLOT_LENGTH = Duration.ofMinutes(30);

	/**
	 * @param segments 그 사용자의 구간들. 서로 겹쳐도 되고, 그날 밖으로 걸쳐도 돼요
	 * @param dailyCap 하루 상한. 넘는 부분은 시간 순으로 뒤에서부터 잘라요
	 */
	public static DailyActivity calculate(Collection<TimeRange> segments, LocalDate date, ZoneId zone,
			Duration dailyCap) {
		ZonedDateTime dayStart = date.atStartOfDay(zone);
		Instant from = dayStart.toInstant();
		Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();

		List<TimeRange> inDay = new ArrayList<>();
		for (TimeRange range : SegmentUnion.of(segments)) {
			TimeRange clipped = range.intersect(from, to);
			if (clipped != null) {
				inDay.add(clipped);
			}
		}
		List<TimeRange> capped = cap(inDay, dailyCap);

		long seconds = capped.stream().mapToLong(range -> range.duration().toSeconds()).sum();
		return new DailyActivity(date, seconds, slots(capped, dayStart));
	}

	/** 앞에서부터 상한만큼만 남겨요. 잠 안 자고 켜 둔 경우처럼 상한을 넘는 날에만 걸려요. */
	private static List<TimeRange> cap(List<TimeRange> ranges, Duration dailyCap) {
		List<TimeRange> capped = new ArrayList<>();
		Duration remaining = dailyCap;
		for (TimeRange range : ranges) {
			if (remaining.isZero()) {
				break;
			}
			if (range.duration().compareTo(remaining) <= 0) {
				capped.add(range);
				remaining = remaining.minus(range.duration());
			} else {
				capped.add(new TimeRange(range.start(), range.start().plus(remaining)));
				remaining = Duration.ZERO;
			}
		}
		return capped;
	}

	private static String slots(List<TimeRange> ranges, ZonedDateTime dayStart) {
		StringBuilder slots = new StringBuilder(SLOT_COUNT);
		for (int i = 0; i < SLOT_COUNT; i++) {
			Instant slotStart = dayStart.plus(SLOT_LENGTH.multipliedBy(i)).toInstant();
			Instant slotEnd = slotStart.plus(SLOT_LENGTH);
			boolean worked = ranges.stream().anyMatch(range -> range.intersect(slotStart, slotEnd) != null);
			slots.append(worked ? '1' : '0');
		}
		return slots.toString();
	}
}
