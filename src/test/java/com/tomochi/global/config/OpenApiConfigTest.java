package com.tomochi.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.support.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

class OpenApiConfigTest {

	@Nested
	@IntegrationTest
	@AutoConfigureMockMvc
	class 로컬_프로필 {

		@Autowired
		MockMvc mockMvc;

		@Test
		void API_문서를_보여준다() throws Exception {
			mockMvc.perform(get("/v3/api-docs"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.info.title").value("Tomochi API"));
		}
	}

	@Nested
	@IntegrationTest
	@AutoConfigureMockMvc
	@ActiveProfiles("prod")
	class 운영_프로필 {

		@Autowired
		MockMvc mockMvc;

		@Test
		void API_문서를_숨긴다() throws Exception {
			mockMvc.perform(get("/v3/api-docs"))
					.andExpect(status().isNotFound());
		}
	}
}
