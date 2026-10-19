package com.tomochi.global.docs;

import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.error.CommonErrorCode;
import com.tomochi.global.error.ErrorCode;
import com.tomochi.global.error.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link ApiErrorCodes}를 읽어 Swagger 응답에 오류 예시를 붙여요. 같은 상태 코드의 오류는 한 응답에 예시 여러 개로 모아요.
 * 어노테이션은 컨트롤러가 구현한 {@code XxxApi} 인터페이스에 있어도 찾아요.
 * 로그인이 필요한 API(공개 표시 {@code @SecurityRequirements}가 없는 API)에는 401 예시를, 모든 API에는 요청 제한 429 예시를
 * 자동으로 붙여요.
 */
@Component
public class ApiErrorCodesCustomizer implements OperationCustomizer {

	private static final String JSON = "application/json";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		List<ErrorCode> errorCodes = new ArrayList<>();
		if (requiresLogin(operation)) {
			errorCodes.addAll(List.of(
					AuthErrorCode.UNAUTHENTICATED, AuthErrorCode.TOKEN_EXPIRED, AuthErrorCode.TOKEN_INVALID));
		}
		errorCodes.add(CommonErrorCode.RATE_LIMITED);
		Set<ApiErrorCodes> declared =
				AnnotatedElementUtils.findMergedRepeatableAnnotations(handlerMethod.getMethod(), ApiErrorCodes.class);
		for (ApiErrorCodes annotation : declared) {
			for (String name : annotation.codes()) {
				errorCodes.add(find(annotation.value(), name));
			}
		}
		Map<Integer, Map<String, Example>> examplesByStatus = new LinkedHashMap<>();
		for (ErrorCode errorCode : errorCodes) {
			examplesByStatus
					.computeIfAbsent(errorCode.status().value(), status -> new LinkedHashMap<>())
					.put(errorCode.code(), new Example()
							.summary(errorCode.message())
							.value(ErrorResponse.of(errorCode)));
		}
		ApiResponses responses = operation.getResponses() != null ? operation.getResponses() : new ApiResponses();
		examplesByStatus.forEach((status, examples) -> responses.addApiResponse(String.valueOf(status),
				errorResponse(responses.get(String.valueOf(status)), examples)));
		return operation.responses(responses);
	}

	/** 전역 Bearer 요구를 따르는 API. {@code @SecurityRequirements}를 붙이면 빈 목록이 되어 공개 API예요. */
	private static boolean requiresLogin(Operation operation) {
		return operation.getSecurity() == null;
	}

	private ApiResponse errorResponse(ApiResponse existing, Map<String, Example> examples) {
		ApiResponse response = existing != null ? existing : new ApiResponse();
		if (response.getDescription() == null || response.getDescription().isBlank()) {
			response.description(String.join(", ", examples.keySet()));
		}
		MediaType mediaType = new MediaType().schema(errorSchema());
		examples.forEach(mediaType::addExamples);
		return response.content(new Content().addMediaType(JSON, mediaType));
	}

	@SuppressWarnings("rawtypes")
	private Schema errorSchema() {
		return ModelConverters.getInstance().resolveAsResolvedSchema(
				new io.swagger.v3.core.converter.AnnotatedType(ErrorResponse.class)).schema;
	}

	private static ErrorCode find(Class<? extends Enum<?>> type, String name) {
		return Arrays.stream(type.getEnumConstants())
				.filter(constant -> constant.name().equals(name))
				.map(ErrorCode.class::cast)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(type.getSimpleName() + "에 " + name + "이 없어요"));
	}
}
