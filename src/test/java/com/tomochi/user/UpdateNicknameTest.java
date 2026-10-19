package com.tomochi.user;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class UpdateNicknameTest {

	@Autowired MockMvc mockMvc;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private String accessToken;

	@BeforeEach
	void setUp() {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	@Test
	void 닉네임을_바꾼다() throws Exception {
		patchMe("{\"nickname\": \"  민지  \"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("민지"));
	}

	@Test
	void 닉네임을_보내지_않으면_그대로_둔다() throws Exception {
		patchMe("{\"nickname\": \"민지\"}").andExpect(status().isOk());

		patchMe("{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("민지"));
	}

	@Test
	void 빈_문자열이나_null을_보내면_닉네임을_지운다() throws Exception {
		patchMe("{\"nickname\": \"민지\"}").andExpect(status().isOk());
		patchMe("{\"nickname\": \"   \"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value(nullValue()));

		patchMe("{\"nickname\": \"민지\"}").andExpect(status().isOk());
		patchMe("{\"nickname\": null}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value(nullValue()));
	}

	@Test
	void 규칙에_맞지_않으면_400이고_바꾸지_않는다() throws Exception {
		patchMe("{\"nickname\": \"민지\"}").andExpect(status().isOk());

		patchMe("{\"nickname\": \"가나다라마바사아자차카타파\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_NICKNAME"));
		patchMe("{\"nickname\": \"민\\n지\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_NICKNAME"));

		patchMe("{}").andExpect(jsonPath("$.nickname").value("민지"));
	}

	@Test
	void 이모지_닉네임을_저장한다() throws Exception {
		patchMe("{\"nickname\": \"초코🍫\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("초코🍫"));
	}

	private ResultActions patchMe(String body) throws Exception {
		return mockMvc.perform(patch("/api/v1/me")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}
