package com.tomochi.activity.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 사용자 시간대 기준 하루의 "함께한 시간"과 30분 칸 48개.
 *
 * @param seconds 그날 작업 구간 합집합의 길이. 하루 상한을 넘으면 상한까지만
 * @param slots   현지 시각 0시부터 30분 단위 48칸. 그 30분에 1초라도 작업이 걸치면 '1'.
 *                서머타임으로 하루가 23·25시간인 날도 칸은 현지 시각 기준이에요
 *                (건너뛴 시간대 칸은 비고, 두 번 지나는 시간대는 같은 칸에 칠해요)
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

		// 구간마다 초로 바꾸면 소수점 아래가 버려져서, 합친 뒤 한 번만 바꿔요.
		long seconds = capped.stream().map(TimeRange::duration).reduce(Duration.ZERO, Duration::plus).toSeconds();
		return new DailyActivity(date, seconds, slots(capped, zone));
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

	/**
	 * 구간을 현지 시각 30분 칸 경계마다 잘라 가며 칸을 칠해요.
	 * 경계는 현지 시각에 30분을 더해 구해요. 지난 시간(Duration)을 더하면 서머타임 날에 칸이 밀려요.
	 */
	private static String slots(List<TimeRange> ranges, ZoneId zone) {
		char[] slots = "0".repeat(SLOT_COUNT).toCharArray();
		for (TimeRange range : ranges) {
			Instant cursor = range.start();
			while (cursor.isBefore(range.end())) {
				ZonedDateTime local = cursor.atZone(zone);
				int minuteOfDay = local.getHour() * 60 + local.getMinute();
				int slot = minuteOfDay / (int) SLOT_LENGTH.toMinutes();
				slots[slot] = '1';
				cursor = nextSlotBoundary(local, slot);
			}
		}
		return new String(slots);
	}

	/**
	 * 다음 칸 경계. 지금 오프셋 그대로 현지 경계 시각을 순간으로 바꿔요.
	 * <ul>
	 *   <li>서머타임 시작(01:40 PST): 02:00 PST = 03:00 PDT로 넘어가 02시대 칸은 비어요</li>
	 *   <li>서머타임 끝(01:40 PDT): 02:00 PDT = 01:00 PST로 넘어가 두 번째 01시대도 같은 칸을 칠해요</li>
	 * </ul>
	 */
	private static Instant nextSlotBoundary(ZonedDateTime local, int slot) {
		LocalDateTime boundary = LocalDateTime.of(local.toLocalDate(), LocalTime.MIDNIGHT)
				.plus(SLOT_LENGTH.multipliedBy(slot + 1L));
		return boundary.toInstant(local.getOffset());
	}
}
