package com.tomochi.activity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.repository.ActivitySegmentWriter;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class WeekApiTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Autowired MockMvc mockMvc;
	@Autowired ActivitySegmentWriter writer;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 이번_주_일요일부터_지금까지_합친다() throws Exception {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		String accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
		LocalDate today = LocalDate.now(SEOUL);
		LocalDate sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

		// 이번 주 일요일 00:00~01:00 (1시간), 지난 주 토요일 23:00~24:00 (안 셈)
		store(userId, sunday.atStartOfDay(SEOUL).toInstant(), 3600);
		store(userId, sunday.atStartOfDay(SEOUL).toInstant().minusSeconds(3600), 3600);

		mockMvc.perform(get("/api/v1/me/weeks/current").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.weekStart").value(sunday.toString()))
				.andExpect(jsonPath("$.seconds").value(3600))
				.andExpect(jsonPath("$.togetherDays").value(0));
	}

	private void store(Long userId, Instant start, long seconds) {
		writer.upsert(userId, Map.of(UUID.randomUUID().toString(), new TimeRange(start, start.plusSeconds(seconds))),
				Instant.now());
	}

	@Test
	void 이번_주에_친구_누구와든_함께한_날을_센다() throws Exception {
		Long me = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		Long minji = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		Long jiwoo = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		befriend(me, minji);
		befriend(me, jiwoo);
		LocalDate today = LocalDate.now(SEOUL);
		LocalDate sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
		// 지난 주 토요일(안 셈), 이번 주 일요일에 두 친구와(1일), 오늘 즉석(1일)
		insertTogether(me, minji, sunday.minusDays(1));
		insertTogether(me, minji, sunday);
		insertTogether(me, jiwoo, sunday);
		Instant start = Instant.now().minusSeconds(1200);
		if (LocalDate.ofInstant(start, SEOUL).isBefore(today)) {
			start = today.atStartOfDay(SEOUL).toInstant();
		}
		store(me, start, 600);
		store(jiwoo, start, 600);

		int expected = sunday.equals(today) ? 1 : 2;
		mockMvc.perform(get("/api/v1/me/weeks/current").header("Authorization",
						"Bearer " + tokenService.issueForNewLogin(me, "맥북").accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.togetherDays").value(expected));
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}

	private void insertTogether(Long owner, Long friend, LocalDate date) {
		jdbcTemplate.update("INSERT INTO together_daily (owner_id, friend_id, date, seconds, updated_at) VALUES (?, ?, ?, 600, UTC_TIMESTAMP(6))",
				owner, friend, date);
	}
}
