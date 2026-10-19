package com.tomochi.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tomochi.global.error.ErrorResponseWriter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

	private final RateLimiter rateLimiter = mock(RateLimiter.class);
	private final RateLimitProperties properties = new RateLimitProperties(true, Duration.ofMinutes(1), 20, 6, 120, 10, 50, 30);
	private final RateLimitFilter filter = new RateLimitFilter(RateLimitFilter.Stage.API, rateLimiter, properties,
			new ErrorResponseWriter(JsonMapper.builder().build()));

	@Test
	void Redis가_응답하지_않으면_막지_않고_통과시킨다() throws Exception {
		given(rateLimiter.acquire(anyString(), anyInt(), any())).willThrow(new IllegalStateException("redis down"));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}
}
