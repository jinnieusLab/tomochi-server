package com.tomochi.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.repository.ActivitySegmentWriter;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DATETIME(6)은 시간대가 없어서, 어떤 JVM 시간대에서 저장해도 UTC 벽시계 값이 들어가야 해요.
 * 드라이버가 connectionTimeZone=UTC로 Instant를 바꿔 보내는지 확인해요.
 */
@IntegrationTest
class StoredTimeZoneTest {

	@Autowired ActivitySegmentWriter writer;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void JVM_시간대와_상관없이_UTC_벽시계로_저장한다() {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		String id = UUID.randomUUID().toString();
		Instant start = Instant.parse("2026-10-17T09:02:11Z");
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
			writer.upsert(userId, Map.of(id, new TimeRange(start, start.plusSeconds(60))), Instant.now());
		} finally {
			TimeZone.setDefault(original);
		}

		String raw = jdbcTemplate.queryForObject(
				"SELECT CAST(started_at AS CHAR) FROM activity_segment WHERE client_segment_id = ?", String.class, id);
		assertThat(raw).startsWith("2026-10-17 09:02:11");
	}

	@Test
	void JPA로_저장한_시각도_UTC_벽시계다() {
		TimeZone original = TimeZone.getDefault();
		Long userId;
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
			userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		} finally {
			TimeZone.setDefault(original);
		}

		java.time.LocalDateTime stored = jdbcTemplate.queryForObject(
				"SELECT created_at FROM users WHERE id = ?", java.time.LocalDateTime.class, userId);
		java.time.LocalDateTime utcNow = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
		assertThat(java.time.Duration.between(stored, utcNow).abs()).isLessThan(java.time.Duration.ofMinutes(1));
	}
}
