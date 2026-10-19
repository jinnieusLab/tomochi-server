package com.tomochi.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 비즈니스_예외는_오류_코드의_상태와_본문으로_바뀐다() throws Exception {
		mockMvc.perform(get("/test/business"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("입력값이 올바르지 않아요"))
				.andExpect(jsonPath("$.retryAfterSeconds").doesNotExist())
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}

	@Test
	void 대기_시간이_있으면_Retry_After_헤더와_본문에_싣는다() throws Exception {
		mockMvc.perform(get("/test/rate-limited"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "7"))
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
				.andExpect(jsonPath("$.retryAfterSeconds").value(7));
	}

	@Test
	void 본문_검증에_실패하면_틀린_필드를_알려준다() throws Exception {
		mockMvc.perform(post("/test/body")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
				.andExpect(jsonPath("$.fieldErrors[0].reason").isNotEmpty());
	}

	@Test
	void 파라미터_검증에_실패하면_틀린_파라미터를_알려준다() throws Exception {
		mockMvc.perform(get("/test/param").param("weeks", "100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("weeks"));
	}

	@Test
	void 파라미터_타입이_틀리면_INVALID_INPUT() throws Exception {
		mockMvc.perform(get("/test/param").param("weeks", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void JSON을_읽을_수_없으면_MALFORMED_REQUEST() throws Exception {
		mockMvc.perform(post("/test/body")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{nickname"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void 없는_주소는_RESOURCE_NOT_FOUND() throws Exception {
		mockMvc.perform(get("/test/nowhere"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void 지원하지_않는_메서드는_METHOD_NOT_ALLOWED() throws Exception {
		mockMvc.perform(delete("/test/business"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void 예상하지_못한_예외는_내부_메시지를_숨긴다() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("잠시 문제가 생겼어요. 잠시 뒤에 다시 시도해 주세요"));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/business")
		void business() {
			throw new BusinessException(CommonErrorCode.INVALID_INPUT);
		}

		@GetMapping("/test/rate-limited")
		void rateLimited() {
			throw new BusinessException(CommonErrorCode.RATE_LIMITED, 7L);
		}

		@PostMapping("/test/body")
		void body(@Valid @RequestBody NicknameRequest request) {
		}

		@GetMapping("/test/param")
		void param(@RequestParam @Max(17) int weeks) {
		}

		@GetMapping("/test/unexpected")
		void unexpected() {
			throw new IllegalStateException("DB 비밀번호가 틀렸어요");
		}
	}

	record NicknameRequest(@NotBlank String nickname) {
	}
}
