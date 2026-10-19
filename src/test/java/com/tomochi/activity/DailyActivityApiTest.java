package com.tomochi.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.repository.ActivitySegmentWriter;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 구간은 검증을 거치지 않고 저장소로 바로 넣어요. 7일보다 오래된 날(캐시 대상)을 만들 수 있게요.
 */
@IntegrationTest
@AutoConfigureMockMvc
class DailyActivityApiTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired ActivitySegmentWriter writer;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long userId;
	private String accessToken;
	private LocalDate today;

	@BeforeEach
	void setUp() {
		userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
		today = LocalDate.now(SEOUL);
	}

	@Test
	void 그날_함께한_시간과_칸을_준다() throws Exception {
		LocalDate day = today.minusDays(2);
		// 맥북 09:00~10:00, 아이맥 09:30~11:00 → 합집합 2시간
		store(day, "09:00", "10:00");
		store(day, "09:30", "11:00");

		day(day)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.date").value(day.toString()))
				.andExpect(jsonPath("$.seconds").value(7200))
				.andExpect(jsonPath("$.slots").value("0".repeat(18) + "1111" + "0".repeat(26)));
	}

	@Test
	void 기록이_없는_날은_0이다() throws Exception {
		day(today)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.seconds").value(0))
				.andExpect(jsonPath("$.slots").value("0".repeat(48)));
	}

	@Test
	void 자정을_넘는_구간은_두_날에_나눠_센다() throws Exception {
		LocalDate day = today.minusDays(3);
		Instant start = day.atTime(23, 0).atZone(SEOUL).toInstant();
		writer.upsert(userId, Map.of(UUID.randomUUID().toString(), new TimeRange(start, start.plusSeconds(7200))),
				Instant.now());

		days(day, day.plusDays(1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days.length()").value(2))
				.andExpect(jsonPath("$.days[0].seconds").value(3600))
				.andExpect(jsonPath("$.days[1].seconds").value(3600));
	}

	@Test
	void 여러_날을_날짜_순으로_빈_날까지_준다() throws Exception {
		store(today.minusDays(4), "10:00", "10:30");

		days(today.minusDays(6), today)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days.length()").value(7))
				.andExpect(jsonPath("$.days[0].date").value(today.minusDays(6).toString()))
				.andExpect(jsonPath("$.days[2].seconds").value(1800))
				.andExpect(jsonPath("$.days[6].date").value(today.toString()));
	}

	@Test
	void 기간은_31일까지다() throws Exception {
		days(today.minusDays(30), today).andExpect(status().isOk()).andExpect(jsonPath("$.days.length()").value(31));
		days(today.minusDays(31), today)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
		days(today, today.minusDays(1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
	}

	@Test
	void 오늘보다_뒤나_6개월보다_전은_404() throws Exception {
		day(today.plusDays(1))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DAY_OUT_OF_RANGE"));
		day(today.minusDays(187))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DAY_OUT_OF_RANGE"));
		day(today.minusDays(186)).andExpect(status().isOk());
	}

	@Test
	void 날짜_형식이_틀리면_400() throws Exception {
		mockMvc.perform(get("/api/v1/me/days/2026-13-01").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 확정된_지난_날만_캐시한다() throws Exception {
		LocalDate old = today.minusDays(10);
		LocalDate recent = today.minusDays(2);
		store(old, "09:00", "10:00");
		store(recent, "09:00", "10:00");

		days(old, today).andExpect(status().isOk());

		assertThat(cachedDates()).contains(old).doesNotContain(recent, today);
		assertThat(cachedDates()).allMatch(date -> date.isBefore(today.minusDays(7)));
	}

	@Test
	void 캐시한_날은_다시_계산하지_않는다() throws Exception {
		LocalDate old = today.minusDays(10);
		store(old, "09:00", "10:00");
		day(old).andExpect(jsonPath("$.seconds").value(3600));

		// 업로드로는 들어올 수 없는 늦은 구간을 억지로 넣어도, 확정된 날이라 캐시 값을 그대로 줘요.
		store(old, "13:00", "14:00");

		day(old).andExpect(jsonPath("$.seconds").value(3600));
	}

	@Test
	void 최근_날은_늦게_올라온_구간을_바로_반영한다() throws Exception {
		LocalDate recent = today.minusDays(2);
		store(recent, "09:00", "10:00");
		day(recent).andExpect(jsonPath("$.seconds").value(3600));

		store(recent, "13:00", "14:00");

		day(recent).andExpect(jsonPath("$.seconds").value(7200));
	}

	@Test
	void 시간대를_바꾸면_캐시를_버리고_새_시간대로_계산한다() throws Exception {
		LocalDate old = today.minusDays(10);
		// 서울 10시~11시 = UTC 01시~02시 = LA 전날 18시~19시
		store(old, "10:00", "11:00");
		day(old).andExpect(jsonPath("$.seconds").value(3600));

		mockMvc.perform(patch("/api/v1/me")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"timezone\": \"America/Los_Angeles\"}"))
				.andExpect(status().isOk());

		day(old).andExpect(jsonPath("$.seconds").value(0));
		day(old.minusDays(1)).andExpect(jsonPath("$.seconds").value(3600));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT timezone FROM daily_activity WHERE user_id = ? AND date = ?", String.class, userId, old))
				.isEqualTo("America/Los_Angeles");
	}

	@Test
	void 로그인하지_않으면_볼_수_없다() throws Exception {
		mockMvc.perform(get("/api/v1/me/days/" + today)).andExpect(status().isUnauthorized());
	}

	private void store(LocalDate date, String start, String end) {
		writer.upsert(userId, Map.of(UUID.randomUUID().toString(), new TimeRange(
				date.atTime(LocalTime.parse(start)).atZone(SEOUL).toInstant(),
				date.atTime(LocalTime.parse(end)).atZone(SEOUL).toInstant())), Instant.now());
	}

	private ResultActions day(LocalDate date) throws Exception {
		return mockMvc.perform(get("/api/v1/me/days/" + date).header("Authorization", "Bearer " + accessToken));
	}

	private ResultActions days(LocalDate from, LocalDate to) throws Exception {
		return mockMvc.perform(get("/api/v1/me/days")
				.param("from", from.toString())
				.param("to", to.toString())
				.header("Authorization", "Bearer " + accessToken));
	}

	private java.util.List<LocalDate> cachedDates() {
		return jdbcTemplate.queryForList("SELECT date FROM daily_activity WHERE user_id = ?", LocalDate.class, userId);
	}
}
