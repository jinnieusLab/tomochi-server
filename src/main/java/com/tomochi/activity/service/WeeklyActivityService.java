package com.tomochi.activity.service;

import com.tomochi.activity.dto.DayResponse;
import com.tomochi.activity.dto.WeekResponse;
import com.tomochi.global.error.BusinessException;
import com.tomochi.together.service.TogetherQueryService;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.error.UserErrorCode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이번 주 합계. 주는 일요일에 시작해요 (앱의 잔디·이번 주 계산과 같아요).
 * 하루 기록과 같은 계산·캐시를 써서, 하루 상자 숫자를 더한 값과 이번 주 숫자가 늘 같아요.
 */
@Service
@RequiredArgsConstructor
public class WeeklyActivityService {

	private final UserRepository userRepository;
	private final DailyActivityService dailyActivityService;
	private final TogetherQueryService togetherQueryService;
	private final Clock clock;

	@Transactional
	public WeekResponse findCurrentWeek(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
		LocalDate weekStart = weekStart(today);

		long seconds = dailyActivityService.findDays(userId, weekStart, today).stream()
				.mapToLong(DayResponse::seconds)
				.sum();
		int togetherDays = togetherQueryService.countTogetherDays(userId, weekStart, today);
		return new WeekResponse(weekStart, seconds, togetherDays);
	}

	static LocalDate weekStart(LocalDate date) {
		return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
	}
}
