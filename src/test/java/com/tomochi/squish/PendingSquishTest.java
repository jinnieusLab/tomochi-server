package com.tomochi.squish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.squish.batch.SquishCleanupJob;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class PendingSquishTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired SquishCleanupJob cleanupJob;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long me;

	@BeforeEach
	void setUp() {
		me = newUser();
	}

	@Test
	void 친구별로_묶어_최근에_보낸_친구부터_준다() throws Exception {
		Long minji = newUser();
		Long jiwoo = newUser();
		insertSquish(minji, me, 30);
		insertSquish(minji, me, 20);
		insertSquish(jiwoo, me, 10);

		JsonNode items = pending().get("items");

		assertThat(items).hasSize(2);
		assertThat(items.get(0).get("fromFriendId").asLong()).isEqualTo(jiwoo);
		assertThat(items.get(1).get("fromFriendId").asLong()).isEqualTo(minji);
		assertThat(items.get(1).get("count").asInt()).isEqualTo(2);
		assertThat(items.get(1).get("squishIds")).hasSize(2);
	}

	@Test
	void ack하면_다음에는_오지_않는다() throws Exception {
		Long minji = newUser();
		insertSquish(minji, me, 5);
		List<Long> ids = jsonMapper.convertValue(pending().at("/items/0/squishIds"),
				jsonMapper.getTypeFactory().constructCollectionType(List.class, Long.class));

		ack(me, ids).andExpect(status().isNoContent());
		ack(me, ids).andExpect(status().isNoContent());

		assertThat(pending().get("items")).isEmpty();
	}

	@Test
	void 남의_꾹은_ack해도_그대로다() throws Exception {
		Long minji = newUser();
		insertSquish(minji, me, 5);
		Long id = pending().at("/items/0/squishIds/0").asLong();

		ack(newUser(), List.of(id)).andExpect(status().isNoContent());

		assertThat(pending().get("items")).hasSize(1);
	}

	@Test
	void 삼십일이_지난_꾹과_탈퇴한_친구의_꾹은_없다() throws Exception {
		Long old = newUser();
		Long gone = newUser();
		insertSquishDaysAgo(old, me, 31);
		insertSquish(gone, me, 5);
		jdbcTemplate.update("UPDATE users SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", gone);

		assertThat(pending().get("items")).isEmpty();
	}

	@Test
	void 정리_배치는_보낸_지_30일_지난_꾹을_지운다() {
		Long minji = newUser();
		insertSquishDaysAgo(minji, me, 31);
		insertSquish(minji, me, 5);

		cleanupJob.run();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM squish WHERE to_user_id = ?", Integer.class, me))
				.isEqualTo(1);
	}

	private JsonNode pending() throws Exception {
		return jsonMapper.readTree(mockMvc.perform(get("/api/v1/squishes/pending").header("Authorization", bearer(me)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
	}

	private org.springframework.test.web.servlet.ResultActions ack(Long userId, List<Long> ids) throws Exception {
		return mockMvc.perform(post("/api/v1/squishes/ack").header("Authorization", bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(java.util.Map.of("squishIds", ids))));
	}

	private void insertSquish(Long from, Long to, int minutesAgo) {
		jdbcTemplate.update("INSERT INTO squish (from_user_id, to_user_id, sent_at) VALUES (?, ?, UTC_TIMESTAMP(6) - INTERVAL ? MINUTE)",
				from, to, minutesAgo);
	}

	private void insertSquishDaysAgo(Long from, Long to, int daysAgo) {
		jdbcTemplate.update("INSERT INTO squish (from_user_id, to_user_id, sent_at) VALUES (?, ?, UTC_TIMESTAMP(6) - INTERVAL ? DAY)",
				from, to, daysAgo);
	}

	private String bearer(Long userId) {
		return "Bearer " + tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}
}
