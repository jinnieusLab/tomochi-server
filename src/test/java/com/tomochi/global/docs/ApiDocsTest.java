package com.tomochi.global.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tomochi.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 인터페이스에 적은 명세가 OpenAPI 문서에 실제로 나오는지 확인해요.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ApiDocsTest {

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;

	private JsonNode paths;
	private JsonNode schemas;

	@BeforeEach
	void setUp() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
		JsonNode docs = jsonMapper.readTree(body);
		paths = docs.get("paths");
		schemas = docs.at("/components/schemas");
	}

	@Test
	void 오류_코드_예시가_상태_코드별로_붙는다() {
		JsonNode responses = paths.at("/~1api~1v1~1auth~1refresh/post/responses");

		assertThat(responses.at("/401/content/application~1json/examples").has("REFRESH_TOKEN_INVALID")).isTrue();
		assertThat(responses.at("/409/content/application~1json/examples/REFRESH_TOKEN_ROTATED/value/code").asString())
				.isEqualTo("REFRESH_TOKEN_ROTATED");
		assertThat(responses.at("/403/content/application~1json/examples").has("REFRESH_TOKEN_REUSED")).isTrue();
		assertThat(responses.at("/400/content/application~1json/examples").has("INVALID_INPUT")).isTrue();
	}

	@Test
	void 로그인이_필요한_API에는_401이_자동으로_붙는다() {
		JsonNode examples = paths.at("/~1api~1v1~1auth~1sessions/get/responses/401/content/application~1json/examples");

		assertThat(examples.has("UNAUTHENTICATED")).isTrue();
		assertThat(examples.has("TOKEN_EXPIRED")).isTrue();
		assertThat(examples.has("TOKEN_INVALID")).isTrue();
	}

	@Test
	void 공개_API에는_401이_없다() {
		assertThat(paths.at("/~1api~1v1~1app~1version/get/responses").has("401")).isFalse();
		assertThat(paths.at("/~1api~1v1~1auth~1token/post/security").isArray()).isTrue();
	}

	@Test
	void 로그인한_사용자는_파라미터로_보이지_않는다() {
		JsonNode parameters = paths.at("/~1api~1v1~1auth~1sessions/get/parameters");

		assertThat(parameters.isMissingNode() || parameters.isEmpty()).isTrue();
	}

	@Test
	void 로그인_시작_주소도_문서에_있다() {
		JsonNode startLogin = paths.at("/~1api~1v1~1auth~1oauth2~1{provider}/get");

		assertThat(startLogin.at("/summary").asString()).contains("소셜 로그인 시작");
		assertThat(startLogin.at("/responses").has("302")).isTrue();
	}

	@Test
	void 내_정보_바꾸기의_검증_오류가_400에_모인다() {
		JsonNode examples = paths.at("/~1api~1v1~1me/patch/responses/400/content/application~1json/examples");

		assertThat(examples.has("INVALID_NICKNAME")).isTrue();
		assertThat(examples.has("INVALID_PET_KIND")).isTrue();
		assertThat(examples.has("INVALID_TIMEZONE")).isTrue();
	}

	@Test
	void 내_정보_바꾸기_요청은_세_필드만_보인다() {
		JsonNode properties = schemas.at("/UpdateUserRequest/properties");

		assertThat(properties.propertyNames()).containsExactlyInAnyOrder("nickname", "petKind", "timezone");
	}

	@Test
	void 구간_올리기_명세가_있다() {
		JsonNode upload = paths.at("/~1api~1v1~1activity~1segments/post");

		assertThat(upload.at("/tags/0").asString()).isEqualTo("활동 기록");
		assertThat(upload.at("/responses/400/content/application~1json/examples").has("INVALID_INPUT")).isTrue();
		assertThat(upload.at("/responses/401/content/application~1json/examples").has("TOKEN_EXPIRED")).isTrue();
		assertThat(schemas.at("/SegmentRequest/properties/clientSegmentId/example").asString()).isNotBlank();
	}
}
