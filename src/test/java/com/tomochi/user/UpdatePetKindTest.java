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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class UpdatePetKindTest {

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
	@EnumSource(PetKind.class)
	void 여섯_가지_펫으로_바꿀_수_있다(PetKind petKind) throws Exception {
		patchMe("{\"petKind\": \"" + petKind.name() + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.petKind").value(petKind.name()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"MOCHI\"", "\"hobbang\"", "\"\"", "null"})
	void 없는_펫이나_빈_값은_400이고_바꾸지_않는다(String value) throws Exception {
		patchMe("{\"petKind\": " + value + "}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PET_KIND"));

		patchMe("{}").andExpect(jsonPath("$.petKind").value("CHOCO"));
	}

	@Test
	void 펫과_닉네임을_한_번에_바꾼다() throws Exception {
		patchMe("{\"nickname\": \"민지\", \"petKind\": \"BUTTER\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("민지"))
				.andExpect(jsonPath("$.petKind").value("BUTTER"));
	}

	@Test
	void 둘_중_하나가_틀리면_아무것도_바꾸지_않는다() throws Exception {
		patchMe("{\"nickname\": \"민지\", \"petKind\": \"MOCHI\"}")
				.andExpect(status().isBadRequest());

		patchMe("{}")
				.andExpect(jsonPath("$.nickname").doesNotExist())
				.andExpect(jsonPath("$.petKind").value("CHOCO"));
	}

	private ResultActions patchMe(String body) throws Exception {
		return mockMvc.perform(patch("/api/v1/me")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}
