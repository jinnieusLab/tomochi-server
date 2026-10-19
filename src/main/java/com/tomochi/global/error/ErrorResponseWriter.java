package com.tomochi.global.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 컨트롤러 밖(보안 필터)에서 난 오류도 {@link ErrorResponse} 형식으로 써요.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		write(response, errorCode, ErrorResponse.of(errorCode));
	}

	public void write(HttpServletResponse response, ErrorCode errorCode, long retryAfterSeconds) throws IOException {
		write(response, errorCode, ErrorResponse.of(errorCode, retryAfterSeconds));
	}

	private void write(HttpServletResponse response, ErrorCode errorCode, ErrorResponse body) throws IOException {
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), body);
	}
}
