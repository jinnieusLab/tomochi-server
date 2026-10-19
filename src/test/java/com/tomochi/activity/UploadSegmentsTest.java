package com.tomochi.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class UploadSegmentsTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long userId;
	private String accessToken;
	private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	@BeforeEach
	void setUp() {
		userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	@Test
	void 구간을_저장한다() throws Exception {
		String id = UUID.randomUUID().toString();

		upload(segment(id, ago(120), ago(60)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accepted").value(1))
				.andExpect(jsonPath("$.adjusted").value(0))
				.andExpect(jsonPath("$.rejected").isEmpty());

		assertThat(stored(id)).isEqualTo(List.of(ago(120), ago(60)));
	}

	@Test
	void 같은_요청을_다시_보내도_한_번만_저장한다() throws Exception {
		String id = UUID.randomUUID().toString();

		upload(segment(id, ago(120), ago(60))).andExpect(status().isOk());
		upload(segment(id, ago(120), ago(60))).andExpect(status().isOk());

		assertThat(count()).isEqualTo(1);
		assertThat(stored(id)).isEqualTo(List.of(ago(120), ago(60)));
	}

	@Test
	void 같은_id로_늘려_보내면_넓히기만_한다() throws Exception {
		String id = UUID.randomUUID().toString();

		upload(segment(id, ago(120), ago(90))).andExpect(status().isOk());
		upload(segment(id, ago(120), ago(30))).andExpect(status().isOk());
		// 늦게 도착한 옛 요청이 범위를 줄이지 않아요.
		upload(segment(id, ago(120), ago(100))).andExpect(status().isOk());

		assertThat(stored(id)).isEqualTo(List.of(ago(120), ago(30)));
	}

	@Test
	void 한_요청에_같은_id가_두_번_오면_합친다() throws Exception {
		String id = UUID.randomUUID().toString();

		upload(segment(id, ago(120), ago(90)), segment(id, ago(100), ago(60)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accepted").value(2));

		assertThat(count()).isEqualTo(1);
		assertThat(stored(id)).isEqualTo(List.of(ago(120), ago(60)));
	}

	@Test
	void 틀린_구간만_거절하고_나머지는_저장한다() throws Exception {
		String ok = UUID.randomUUID().toString();
		String backwards = UUID.randomUUID().toString();
		String old = UUID.randomUUID().toString();
		String tooLong = UUID.randomUUID().toString();

		upload(
				segment(ok, ago(120), ago(60)),
				segment(backwards, ago(60), ago(120)),
				segment(old, now.minus(Duration.ofDays(8)), now.minus(Duration.ofDays(8)).plusSeconds(600)),
				segment(tooLong, now.minus(Duration.ofHours(14)), now.minus(Duration.ofMinutes(30))),
				segment("not-a-uuid", ago(120), ago(60)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accepted").value(1))
				.andExpect(jsonPath("$.rejected.length()").value(4))
				.andExpect(jsonPath("$.rejected[?(@.clientSegmentId == '" + backwards + "')].reason").value("INVALID_RANGE"))
				.andExpect(jsonPath("$.rejected[?(@.clientSegmentId == '" + old + "')].reason").value("TOO_OLD"))
				.andExpect(jsonPath("$.rejected[?(@.clientSegmentId == '" + tooLong + "')].reason").value("TOO_LONG"))
				.andExpect(jsonPath("$.rejected[?(@.clientSegmentId == 'not-a-uuid')].reason").value("INVALID_ID"));

		assertThat(count()).isEqualTo(1);
	}

	@Test
	void 미래로_간_끝은_서버_시각으로_잘라_저장한다() throws Exception {
		String id = UUID.randomUUID().toString();

		upload(segment(id, ago(60), now.plus(Duration.ofHours(1))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accepted").value(1))
				.andExpect(jsonPath("$.adjusted").value(1));

		Instant storedEnd = stored(id).get(1);
		assertThat(storedEnd).isBefore(now.plus(Duration.ofMinutes(1))).isAfter(now.minusSeconds(60));
	}

	@Test
	void 다른_사용자의_같은_id와_섞이지_않는다() throws Exception {
		String id = UUID.randomUUID().toString();
		Long other = oauthLoginFacade.login(OAuthProvider.GOOGLE, UUID.randomUUID().toString(), PetKind.CHOCO);
		String otherToken = tokenService.issueForNewLogin(other, "남의 맥").accessToken();

		upload(segment(id, ago(120), ago(60))).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/activity/segments")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(segment(id, ago(30), ago(10)))))
				.andExpect(status().isOk());

		assertThat(stored(id)).isEqualTo(List.of(ago(120), ago(60)));
	}

	@Test
	void 한_번에_200개까지만_받는다() throws Exception {
		List<Map<String, Object>> segments = new ArrayList<>();
		for (int i = 0; i < 201; i++) {
			segments.add(segment(UUID.randomUUID().toString(), ago(120), ago(60)));
		}

		upload(segments.toArray(Map[]::new))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		upload(segments.subList(0, 200).toArray(Map[]::new))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accepted").value(200));
	}

	@Test
	void 로그인하지_않으면_올릴_수_없다() throws Exception {
		mockMvc.perform(post("/api/v1/activity/segments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(segment(UUID.randomUUID().toString(), ago(120), ago(60)))))
				.andExpect(status().isUnauthorized());
	}

	private Instant ago(long minutes) {
		return now.minus(Duration.ofMinutes(minutes));
	}

	private static Map<String, Object> segment(String id, Instant start, Instant end) {
		return Map.of("clientSegmentId", id, "start", start.toString(), "end", end.toString());
	}

	@SafeVarargs
	private ResultActions upload(Map<String, Object>... segments) throws Exception {
		return mockMvc.perform(post("/api/v1/activity/segments")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(segments)));
	}

	@SafeVarargs
	private String body(Map<String, Object>... segments) {
		return jsonMapper.writeValueAsString(Map.of("segments", List.of(segments)));
	}

	private List<Instant> stored(String clientSegmentId) {
		return jdbcTemplate.queryForObject(
				"SELECT started_at, ended_at FROM activity_segment WHERE user_id = ? AND client_segment_id = ?",
				(rs, n) -> List.of(rs.getTimestamp(1).toInstant(), rs.getTimestamp(2).toInstant()),
				userId, clientSegmentId);
	}

	private Integer count() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activity_segment WHERE user_id = ?", Integer.class, userId);
	}
}
