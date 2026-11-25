package com.tomochi.user;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class UpdateDecorationTest {

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
	void 처음에는_장식이_없다() throws Exception {
		patchMe("{}").andExpect(jsonPath("$.decoration").doesNotExist());
	}

	@Test
	void 장식을_얹고_다른_필드를_바꿔도_그대로다() throws Exception {
		patchMe("{\"decoration\": \"winter-scarf\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.decoration").value("winter-scarf"));

		patchMe("{\"nickname\": \"민지\"}")
				.andExpect(jsonPath("$.decoration").value("winter-scarf"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "\"\""})
	void null이나_빈_문자열이면_장식을_벗는다(String value) throws Exception {
		patchMe("{\"decoration\": \"snowman\"}");

		patchMe("{\"decoration\": " + value + "}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.decoration").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"Winter-Scarf\"", "\"winter scarf\"", "\"-scarf\"", "\"scarf-\"", "\"a--b\"", "\"<script>\"",
			"\"abcdefghijabcdefghijabcdefghijabc\""})
	void 모양이_틀린_id는_400이고_바꾸지_않는다(String value) throws Exception {
		patchMe("{\"decoration\": \"snowman\"}");

		patchMe("{\"decoration\": " + value + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DECORATION"));

		patchMe("{}").andExpect(jsonPath("$.decoration").value("snowman"));
	}

	private ResultActions patchMe(String body) throws Exception {
		return mockMvc.perform(patch("/api/v1/me")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}
