package com.tomochi.friend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.service.PresenceStore;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class FriendProfileTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired PresenceStore presenceStore;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	@Test
	void 친구_프로필을_보여준다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.SHERBET);
		befriend(me, minji);
		presenceStore.update(minji, "mac", PresenceStatus.RESTING, null, Instant.now());

		mockMvc.perform(get("/api/v1/friends/{id}", minji).header("Authorization", bearer(me)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(minji))
				.andExpect(jsonPath("$.petKind").value("SHERBET"))
				.andExpect(jsonPath("$.presence.status").value("RESTING"));
	}

	@Test
	void 숨긴_친구도_볼_수_있다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);
		befriend(me, minji);
		jdbcTemplate.update("INSERT INTO friend_setting (owner_id, friend_id, hidden, updated_at) VALUES (?, ?, TRUE, UTC_TIMESTAMP(6))",
				me, minji);

		mockMvc.perform(get("/api/v1/friends/{id}", minji).header("Authorization", bearer(me)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hidden").value(true));
	}

	@Test
	void 친구가_아니면_404() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long stranger = newUser(PetKind.CHOCO);

		mockMvc.perform(get("/api/v1/friends/{id}", stranger).header("Authorization", bearer(me)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/friends/{id}", 999_999_999L).header("Authorization", bearer(me)))
				.andExpect(status().isNotFound());
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
