package com.tomochi.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Actuator는 API 포트가 아니라 관리 포트에서만 열려요. 실제 포트를 띄워서 확인해요.
 * 실제 포트가 필요해서 {@code @IntegrationTest}(MOCK 환경의 {@code @SpringBootTest}) 대신 직접 선언해요.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "management.server.port=0")
// 테스트에서는 기본으로 Prometheus 같은 외부 지표 저장소를 구성하지 않아서 켜요.
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class ManagementEndpointTest {

	@Value("${local.server.port}")
	int serverPort;

	@Value("${local.management.port}")
	int managementPort;

	private final RestClient restClient = RestClient.builder()
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
			.build();

	@Test
	void 관리_포트에서_DB와_Redis_상태를_알려준다() {
		ResponseEntity<String> health = get(managementPort, "/actuator/health");

		assertThat(health.getStatusCode().value()).isEqualTo(200);
		assertThat(health.getBody()).contains("\"status\":\"UP\"", "\"db\"", "\"redis\"");
	}

	@Test
	void 관리_포트에서_Prometheus_지표를_내보낸다() {
		get(serverPort, "/v3/api-docs");

		ResponseEntity<String> metrics = get(managementPort, "/actuator/prometheus");

		assertThat(metrics.getStatusCode().value()).isEqualTo(200);
		assertThat(metrics.getBody())
				.contains("jvm_memory_used_bytes")
				.contains("http_server_requests_seconds")
				.contains("application=\"tomochi-server\"");
	}

	@Test
	void API_포트에서는_Actuator를_볼_수_없다() {
		ResponseEntity<String> health = get(serverPort, "/actuator/health");
		ResponseEntity<String> metrics = get(serverPort, "/actuator/prometheus");

		assertThat(health.getStatusCode().is2xxSuccessful()).isFalse();
		assertThat(metrics.getStatusCode().is2xxSuccessful()).isFalse();
		assertThat(metrics.getBody()).doesNotContain("jvm_memory_used_bytes");
	}

	private ResponseEntity<String> get(int port, String path) {
		return restClient.get().uri("http://localhost:" + port + path).retrieve().toEntity(String.class);
	}
}
