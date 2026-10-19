package com.tomochi.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.friend.service.FriendSettingService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class HideFriendTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired FriendSettingService friendSettingService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	@Test
	void 숨기면_내_목록에서만_빠지고_상대_목록은_그대로다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);

		mockMvc.perform(put("/api/v1/friends/{id}/hidden", minji).header("Authorization", bearer(me)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/friends").header("Authorization", bearer(me)))
				.andExpect(jsonPath("$.friends").isEmpty())
				.andExpect(jsonPath("$.hiddenCount").value(1));
		mockMvc.perform(get("/api/v1/friends").header("Authorization", bearer(minji)))
				.andExpect(jsonPath("$.friends.length()").value(1))
				.andExpect(jsonPath("$.friends[0].hidden").value(false));
		assertThat(friendSettingService.isHidden(me, minji)).isTrue();
		assertThat(friendSettingService.isHidden(minji, me)).isFalse();
	}

	@Test
	void 다시_보이면_목록에_돌아온다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		mockMvc.perform(put("/api/v1/friends/{id}/hidden", minji).header("Authorization", bearer(me)));
		mockMvc.perform(put("/api/v1/friends/{id}/hidden", minji).header("Authorization", bearer(me)))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/friends/{id}/hidden", minji).header("Authorization", bearer(me)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/friends").header("Authorization", bearer(me)))
				.andExpect(jsonPath("$.friends.length()").value(1))
				.andExpect(jsonPath("$.hiddenCount").value(0));
		assertThat(friendSettingService.isHidden(me, minji)).isFalse();
	}

	@Test
	void 친구가_아니면_404() throws Exception {
		Long me = newUser();
		Long stranger = newUser();

		mockMvc.perform(put("/api/v1/friends/{id}/hidden", stranger).header("Authorization", bearer(me)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND"));
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
