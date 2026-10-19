package com.tomochi.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class InfraConnectionTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	StringRedisTemplate redisTemplate;

	@Test
	void MySQL은_utf8mb4와_UTC로_동작한다() {
		String charset = jdbcTemplate.queryForObject("SELECT @@character_set_database", String.class);
		String sessionTimeZone = jdbcTemplate.queryForObject("SELECT @@session.time_zone", String.class);

		assertThat(charset).isEqualTo("utf8mb4");
		assertThat(sessionTimeZone).isIn("+00:00", "UTC");
	}

	@Test
	void Flyway가_스키마_기록_테이블을_만든다() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'",
				Integer.class);

		assertThat(count).isEqualTo(1);
	}

	@Test
	void Redis에_값을_쓰고_읽는다() {
		redisTemplate.opsForValue().set("test:ping", "pong");

		assertThat(redisTemplate.opsForValue().get("test:ping")).isEqualTo("pong");
	}
}
