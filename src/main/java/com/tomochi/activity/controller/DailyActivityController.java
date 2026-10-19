package com.tomochi.activity.controller;

import com.tomochi.activity.dto.DayResponse;
import com.tomochi.activity.dto.DaysResponse;
import com.tomochi.activity.service.DailyActivityService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/days")
@RequiredArgsConstructor
public class DailyActivityController implements DailyActivityApi {

	private final DailyActivityService dailyActivityService;

	@Override
	@GetMapping("/{date}")
	public DayResponse findDay(@CurrentUser AuthUser user, @PathVariable LocalDate date) {
		return dailyActivityService.findDay(user.userId(), date);
	}

	@Override
	@GetMapping
	public DaysResponse findDays(@CurrentUser AuthUser user, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return new DaysResponse(dailyActivityService.findDays(user.userId(), from, to));
	}
}
