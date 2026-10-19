package com.tomochi.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * docker-compose.yml과 같은 이미지로 MySQL·Redis를 띄워요.
 * {@link ServiceConnection}이 접속 정보를 스프링 설정에 바로 넣어 줘요.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
				.withCommand(
						"--character-set-server=utf8mb4",
						"--collation-server=utf8mb4_unicode_ci",
						"--default-time-zone=+00:00");
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
				.withExposedPorts(6379);
	}
}
