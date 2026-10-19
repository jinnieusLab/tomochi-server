package com.tomochi.together;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class TogetherApiTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired ActivitySegmentWriter writer;
	@Autowired StringRedisTemplate redisTemplate;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long me;
	private Long minji;
	private LocalDate today;

	@BeforeEach
	void setUp() {
		me = newUser();
		minji = newUser();
		befriend(me, minji);
		today = LocalDate.now(SEOUL);
	}

	@Test
	void 기간은_17주이고_일요일에_시작한다() throws Exception {
		LocalDate sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

		together(17)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.from").value(sunday.minusWeeks(16).toString()))
				.andExpect(jsonPath("$.to").value(today.toString()))
				.andExpect(jsonPath("$.days").isEmpty())
				.andExpect(jsonPath("$.totalDays").value(0));
	}

	@Test
	void 지난_날은_저장된_집계를_분으로_준다() throws Exception {
		insertTogether(me, minji, today.minusDays(10), 6000);
		insertTogether(me, minji, today.minusDays(5), 30);
		insertTogether(minji, me, today.minusDays(3), 9999);

		JsonNode body = body(together(17));

		assertThat(body.get("days")).hasSize(2);
		assertThat(body.at("/days/0/date").asString()).isEqualTo(today.minusDays(10).toString());
		assertThat(body.at("/days/0/minutes").asLong()).isEqualTo(100);
		assertThat(body.at("/days/1/minutes").asLong()).as("30초도 1분으로 보여요").isEqualTo(1);
		assertThat(body.get("totalDays").asInt()).isEqualTo(2);
	}

	@Test
	void 오늘은_즉석으로_계산한다() throws Exception {
		Instant start = Instant.now().minusSeconds(3600);
		if (LocalDate.ofInstant(start, SEOUL).isBefore(today)) {
			start = today.atStartOfDay(SEOUL).toInstant();
		}
		store(me, start, start.plusSeconds(1200));
		store(minji, start.plusSeconds(600), start.plusSeconds(1800));

		JsonNode body = body(together(17));

		JsonNode last = body.get("days").get(body.get("days").size() - 1);
		assertThat(last.get("date").asString()).isEqualTo(today.toString());
		assertThat(last.get("minutes").asLong()).isEqualTo(10);
		assertThat(redisTemplate.hasKey("together:today:" + me + ":" + minji + ":" + today)).isTrue();
	}

	@Test
	void 범위를_벗어난_주는_400이고_친구가_아니면_404() throws Exception {
		together(18).andExpect(status().isBadRequest());
		together(0).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/friends/{id}/together", newUser()).header("Authorization", bearer(me)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND"));
	}

	@Test
	void 주를_줄이면_기간도_줄어든다() throws Exception {
		insertTogether(me, minji, today.minusDays(40), 600);
		LocalDate sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

		JsonNode body = body(together(1));

		assertThat(body.get("from").asString()).isEqualTo(sunday.toString());
		assertThat(body.get("days")).isEmpty();
	}

	private ResultActions together(int weeks) throws Exception {
		return mockMvc.perform(get("/api/v1/friends/{id}/together", minji).param("weeks", String.valueOf(weeks))
				.header("Authorization", bearer(me)));
	}

	private JsonNode body(ResultActions actions) throws Exception {
		return jsonMapper.readTree(actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
	}

	private void insertTogether(Long owner, Long friend, LocalDate date, int seconds) {
		jdbcTemplate.update("INSERT INTO together_daily (owner_id, friend_id, date, seconds, updated_at) VALUES (?, ?, ?, ?, UTC_TIMESTAMP(6))",
				owner, friend, date, seconds);
	}

	private void store(Long userId, Instant start, Instant end) {
		writer.upsert(userId, Map.of(UUID.randomUUID().toString(), new TimeRange(start, end)), Instant.now());
	}

	private String bearer(Long userId) {
		return "Bearer " + tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}
}
