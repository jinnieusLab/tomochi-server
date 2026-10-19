package com.tomochi.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.presence.service.PresenceStore;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class FriendListTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired PresenceStore presenceStore;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private Long me;

	@BeforeEach
	void setUp() {
		me = newUser(PetKind.CHOCO);
	}

	@Test
	void 친구와_지금_상태를_보여준다() throws Exception {
		Long working = newUser(PetKind.HOBBANG);
		Long offline = newUser(PetKind.BUTTER);
		Long stranger = newUser(PetKind.PEANUT);
		befriend(me, working);
		befriend(offline, me);
		Instant since = Instant.now().minusSeconds(5100);
		presenceStore.update(working, "mac", PresenceStatus.WORKING, StatusIcon.STUDY, since);
		presenceStore.update(working, "mac", PresenceStatus.WORKING, StatusIcon.STUDY, Instant.now());
		jdbcTemplate.update("UPDATE users SET last_seen_at = UTC_TIMESTAMP(6) - INTERVAL 3 HOUR WHERE id = ?", offline);

		JsonNode body = list(false);

		assertThat(body.get("friends")).hasSize(2);
		JsonNode w = friend(body, working);
		assertThat(w.get("petKind").asString()).isEqualTo("HOBBANG");
		assertThat(w.at("/presence/status").asString()).isEqualTo("WORKING");
		assertThat(w.at("/presence/icon").asString()).isEqualTo("STUDY");
		assertThat(Instant.parse(w.at("/presence/workingSince").asString())).isEqualTo(since.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
		assertThat(w.get("lastSeenAt").isNull()).isTrue();
		JsonNode o = friend(body, offline);
		assertThat(o.at("/presence/status").asString()).isEqualTo("OFFLINE");
		assertThat(o.get("lastSeenAt").isNull()).isFalse();
		assertThat(friend(body, stranger)).isNull();
	}

	@Test
	void 비교_숫자는_넣지_않는다() throws Exception {
		befriend(me, newUser(PetKind.CHOCO));

		JsonNode friend = list(false).get("friends").get(0);

		assertThat(friend.propertyNames())
				.containsExactlyInAnyOrder("id", "nickname", "petKind", "presence", "lastSeenAt", "hidden");
	}

	@Test
	void 숨긴_친구는_빼고_수만_알려준다() throws Exception {
		Long shown = newUser(PetKind.CHOCO);
		Long hidden = newUser(PetKind.CHOCO);
		befriend(me, shown);
		befriend(me, hidden);
		jdbcTemplate.update("INSERT INTO friend_setting (owner_id, friend_id, hidden, updated_at) VALUES (?, ?, TRUE, UTC_TIMESTAMP(6))",
				me, hidden);

		JsonNode body = list(false);
		assertThat(body.get("friends")).hasSize(1);
		assertThat(body.get("hiddenCount").asInt()).isEqualTo(1);
		assertThat(friend(body, hidden)).isNull();

		JsonNode all = list(true);
		assertThat(all.get("friends")).hasSize(2);
		assertThat(friend(all, hidden).get("hidden").asBoolean()).isTrue();
	}

	@Test
	void 탈퇴한_친구는_목록에서_사라진다() throws Exception {
		Long gone = newUser(PetKind.CHOCO);
		befriend(me, gone);
		mockMvc.perform(delete("/api/v1/me").header("Authorization", bearer(gone))).andExpect(status().isNoContent());

		assertThat(list(false).get("friends")).isEmpty();
	}

	@Test
	void 친구가_없으면_빈_목록() throws Exception {
		requestList(false).andExpect(status().isOk())
				.andExpect(jsonPath("$.friends").isEmpty())
				.andExpect(jsonPath("$.hiddenCount").value(0));
	}

	private JsonNode list(boolean includeHidden) throws Exception {
		return jsonMapper.readTree(requestList(includeHidden).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
	}

	private ResultActions requestList(boolean includeHidden) throws Exception {
		return mockMvc.perform(get("/api/v1/friends").param("includeHidden", String.valueOf(includeHidden))
				.header("Authorization", bearer(me)));
	}

	private static JsonNode friend(JsonNode body, Long id) {
		for (JsonNode friend : body.get("friends")) {
			if (friend.get("id").asLong() == id) {
				return friend;
			}
		}
		return null;
	}

	private String bearer(Long userId) {
		return "Bearer " + tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private Long newUser(PetKind petKind) {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), petKind);
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}
}
