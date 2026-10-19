package com.tomochi.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class AddFriendTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired UserRepository userRepository;

	@Test
	void 초대_코드로_바로_친구가_된다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.HOBBANG);

		add(me, codeOf(minji))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(minji))
				.andExpect(jsonPath("$.petKind").value("HOBBANG"));

		assertThat(friendshipCount(me, minji)).isEqualTo(1);
	}

	@Test
	void 코드의_대소문자와_앞뒤_공백은_상관없다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);

		add(me, "  " + codeOf(minji).toLowerCase() + " ").andExpect(status().isOk());

		assertThat(friendshipCount(me, minji)).isEqualTo(1);
	}

	@Test
	void 이미_친구면_어느_쪽에서_보내도_그대로다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);
		add(me, codeOf(minji)).andExpect(status().isOk());

		add(me, codeOf(minji)).andExpect(status().isOk());
		add(minji, codeOf(me)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(me));

		assertThat(friendshipCount(me, minji)).isEqualTo(1);
	}

	@Test
	void 내_코드는_거절한다() throws Exception {
		Long me = newUser(PetKind.CHOCO);

		add(me, codeOf(me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("OWN_INVITE_CODE"));
	}

	@Test
	void 없는_코드나_탈퇴한_사람의_코드는_404() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long gone = newUser(PetKind.CHOCO);
		String goneCode = codeOf(gone);
		mockMvc.perform(delete("/api/v1/me").header("Authorization", bearer(gone))).andExpect(status().isNoContent());

		add(me, "ZZZZZZ").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INVITE_CODE_NOT_FOUND"));
		add(me, goneCode).andExpect(status().isNotFound());
	}

	@Test
	void 상대가_20명이면_409() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long popular = newUser(PetKind.CHOCO);
		giveFriends(popular, 20);

		add(me, codeOf(popular))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("FRIEND_LIMIT_EXCEEDED"));
		assertThat(friendshipCount(me, popular)).isZero();
	}

	@Test
	void 내가_20명이어도_409() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);
		giveFriends(me, 20);

		add(me, codeOf(minji)).andExpect(status().isConflict());
	}

	@Test
	void 친구가_19명이면_한_명_더_맺을_수_있다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);
		giveFriends(me, 19);

		add(me, codeOf(minji)).andExpect(status().isOk());
	}

	private ResultActions add(Long userId, String inviteCode) throws Exception {
		return mockMvc.perform(post("/api/v1/friends")
				.header("Authorization", bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"inviteCode\": \"" + inviteCode + "\"}"));
	}

	private String bearer(Long userId) {
		return "Bearer " + tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private Long newUser(PetKind petKind) {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), petKind);
	}

	private String codeOf(Long userId) {
		return userRepository.findById(userId).orElseThrow().getInviteCode();
	}

	private Integer friendshipCount(Long a, Long b) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friendship WHERE user_low = ? AND user_high = ?",
				Integer.class, Math.min(a, b), Math.max(a, b));
	}

	private void giveFriends(Long userId, int count) {
		for (int i = 0; i < count; i++) {
			Long friend = newUser(PetKind.CHOCO);
			jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
					Math.min(userId, friend), Math.max(userId, friend));
		}
	}
}
