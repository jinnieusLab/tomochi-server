package com.tomochi.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.service.InviteCodeGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class InviteCodeTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired UserRepository userRepository;
	@MockitoSpyBean InviteCodeGenerator inviteCodeGenerator;

	@Test
	void 재발급하면_새_코드로_바뀌고_이전_코드는_사라진다() throws Exception {
		Long userId = newUser();
		String before = userRepository.findById(userId).orElseThrow().getInviteCode();

		JsonNode body = regenerate(userId);

		String after = body.get("inviteCode").asString();
		assertThat(after).hasSize(6).isNotEqualTo(before);
		assertThat(body.get("inviteUrl").asString()).isEqualTo("tomochi://invite/" + after);
		assertThat(userRepository.findById(userId).orElseThrow().getInviteCode()).isEqualTo(after);
		assertThat(userRepository.existsByInviteCode(before)).isFalse();
	}

	@Test
	void 뽑은_코드를_방금_다른_사람이_가져갔으면_다시_뽑는다() throws Exception {
		Long other = newUser();
		String taken = userRepository.findById(other).orElseThrow().getInviteCode();
		Long me = newUser();
		// "안 쓰이는 코드"를 확인한 뒤 다른 사람이 가져간 상황: 첫 번째로 이미 쓰이는 코드를 돌려줘요.
		doReturn(taken).doCallRealMethod().when(inviteCodeGenerator).generateUnique();

		String code = regenerate(me).get("inviteCode").asString();

		assertThat(code).isNotEqualTo(taken);
		assertThat(userRepository.findById(me).orElseThrow().getInviteCode()).isEqualTo(code);
		assertThat(userRepository.findById(other).orElseThrow().getInviteCode()).isEqualTo(taken);
	}

	private JsonNode regenerate(Long userId) throws Exception {
		String accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
		String body = mockMvc.perform(post("/api/v1/me/invite-code/regenerate").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inviteCode").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(body);
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}
}
