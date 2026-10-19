package com.tomochi.activity.service;

import com.tomochi.activity.domain.DailyActivity;
import com.tomochi.activity.domain.SegmentPolicy;
import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.dto.DayResponse;
import com.tomochi.activity.error.ActivityErrorCode;
import com.tomochi.activity.repository.ActivitySegmentReader;
import com.tomochi.activity.repository.DailyActivityCache;
import com.tomochi.global.error.BusinessException;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.error.UserErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 하루 기록 조회. 구간 원본의 합집합으로 계산하고({@link DailyActivity}), 더 이상 바뀔 수 없는 날만 캐시해요.
 *
 * <p>업로드는 시작이 7일보다 전인 구간을 거절해요. 그래서 하루가 끝난 지 7일이 지난 날에는 새 구간이 걸칠 수 없어서,
 * 그날 값은 확정이에요. 확정된 날만 캐시하니 업로드 때 캐시를 지울 필요가 없고, 지우는 순간과 읽는 순간이 엇갈려
 * 옛 값이 남는 경쟁도 없어요. 시간대를 바꾸면 날짜 경계가 달라져서, 캐시에 저장한 시간대가 지금과 다르면 다시 계산해요.
 */
@Service
@RequiredArgsConstructor
public class DailyActivityService {

	public static final int MAX_DAYS_PER_REQUEST = 31;

	private final UserRepository userRepository;
	private final ActivitySegmentReader segmentReader;
	private final DailyActivityCache cache;
	private final Clock clock;

	@Transactional
	public DayResponse findDay(Long userId, LocalDate date) {
		return findDays(userId, date, date).getFirst();
	}

	@Transactional
	public List<DayResponse> findDays(Long userId, LocalDate from, LocalDate to) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		ZoneId zone = ZoneId.of(user.getTimezone());
		Instant now = clock.instant();
		validate(from, to, LocalDate.ofInstant(now, zone));

		Map<LocalDate, DailyActivity> days = cache.find(userId, from, to, user.getTimezone());
		List<LocalDate> missing = from.datesUntil(to.plusDays(1)).filter(date -> !days.containsKey(date)).toList();
		if (!missing.isEmpty()) {
			List<DailyActivity> computed = compute(userId, missing, zone);
			computed.forEach(day -> days.put(day.date(), day));
			List<DailyActivity> settled = computed.stream().filter(day -> isSettled(day.date(), zone, now)).toList();
			if (!settled.isEmpty()) {
				cache.save(userId, settled, user.getTimezone(), now);
			}
		}
		return from.datesUntil(to.plusDays(1)).map(days::get).map(DayResponse::from).toList();
	}

	/** 빠진 날들의 구간을 한 번에 읽어서 날짜별로 계산해요. */
	private List<DailyActivity> compute(Long userId, List<LocalDate> dates, ZoneId zone) {
		Instant from = dates.getFirst().atStartOfDay(zone).toInstant();
		Instant to = dates.getLast().plusDays(1).atStartOfDay(zone).toInstant();
		List<TimeRange> segments = segmentReader.findOverlapping(userId, from, to);

		List<DailyActivity> computed = new ArrayList<>();
		for (LocalDate date : dates) {
			computed.add(DailyActivity.calculate(segments, date, zone, SegmentPolicy.DAILY_CAP));
		}
		return computed;
	}

	/** 그날이 끝난 지 업로드 허용 기간(7일)과 시계 차이보다 오래 지났으면 더 바뀌지 않아요. */
	private static boolean isSettled(LocalDate date, ZoneId zone, Instant now) {
		Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
		return !dayEnd.isAfter(now.minus(SegmentPolicy.MAX_AGE).minus(SegmentPolicy.CLOCK_SKEW));
	}

	private static void validate(LocalDate from, LocalDate to, LocalDate today) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_DAYS_PER_REQUEST) {
			throw new BusinessException(ActivityErrorCode.INVALID_DATE_RANGE);
		}
		if (to.isAfter(today) || from.isBefore(today.minusDays(SegmentPolicy.RETENTION_DAYS))) {
			throw new BusinessException(ActivityErrorCode.DAY_OUT_OF_RANGE);
		}
	}
}
