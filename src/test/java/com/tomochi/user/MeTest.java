package com.tomochi.user;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class MeTest {

	@Autowired MockMvc mockMvc;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired UserRepository userRepository;

	@Test
	void 내_정보를_보여준다() throws Exception {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.SHERBET);
		User user = userRepository.findById(userId).orElseThrow();
		String accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId))
				.andExpect(jsonPath("$.nickname").hasJsonPath())
				.andExpect(jsonPath("$.nickname").value(nullValue()))
				.andExpect(jsonPath("$.petKind").value("SHERBET"))
				.andExpect(jsonPath("$.statusIcon").hasJsonPath())
				.andExpect(jsonPath("$.statusIcon").value(nullValue()))
				.andExpect(jsonPath("$.timezone").value("Asia/Seoul"))
				.andExpect(jsonPath("$.inviteCode").value(user.getInviteCode()))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}
}
