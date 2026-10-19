package com.tomochi.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"tomochi.app.latest=0.4.0", "tomochi.app.minimum=0.3.0"})
class AppVersionApiTest {

	private static final String PROTECTED_PATH = "/api/v1/me";

	@Autowired MockMvc mockMvc;

	@Test
	void 로그인_없이_버전을_알려준다() throws Exception {
		mockMvc.perform(get("/api/v1/app/version"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latest").value("0.4.0"))
				.andExpect(jsonPath("$.minimum").value("0.3.0"))
				.andExpect(jsonPath("$.downloadUrl").isNotEmpty());
	}

	@Test
	void 최소_버전보다_낮으면_인증보다_먼저_426() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header("X-Tomochi-Version", "0.2.9"))
				.andExpect(status().isUpgradeRequired())
				.andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"))
				.andExpect(jsonPath("$.message").value("새 버전을 받아 주세요"));
	}

	@Test
	void 옛_앱도_버전_확인은_할_수_있다() throws Exception {
		mockMvc.perform(get("/api/v1/app/version").header("X-Tomochi-Version", "0.1.0"))
				.andExpect(status().isOk());
	}

	@Test
	void 최소_버전_이상이면_통과한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header("X-Tomochi-Version", "0.3.0"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(PROTECTED_PATH).header("X-Tomochi-Version", "0.10.0"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 헤더가_없거나_형식이_틀리면_통과한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(PROTECTED_PATH).header("X-Tomochi-Version", "dev-build"))
				.andExpect(status().isUnauthorized());
	}
}
