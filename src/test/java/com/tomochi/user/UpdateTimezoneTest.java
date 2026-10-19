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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class UpdateTimezoneTest {

	@Autowired MockMvc mockMvc;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;

	private String accessToken;

	@BeforeEach
	void setUp() {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		accessToken = tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	@ParameterizedTest
	@ValueSource(strings = {"America/Los_Angeles", "Europe/London", "Asia/Tokyo", "UTC"})
	void IANA_시간대로_바꾼다(String timezone) throws Exception {
		patchMe("{\"timezone\": \"" + timezone + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value(timezone));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"Seoul\"", "\"+09:00\"", "\"KST\"", "\"asia/seoul\"", "\"\"", "null"})
	void 이름이_아닌_시간대는_400이고_바꾸지_않는다(String value) throws Exception {
		patchMe("{\"timezone\": " + value + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));

		patchMe("{}").andExpect(jsonPath("$.timezone").value("Asia/Seoul"));
	}

	private ResultActions patchMe(String body) throws Exception {
		return mockMvc.perform(patch("/api/v1/me")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}
