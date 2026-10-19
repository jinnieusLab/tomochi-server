package com.tomochi.activity.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.repository.ActivitySegmentWriter;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class ActivityRetentionJobTest {

	@Autowired ActivityRetentionJob job;
	@Autowired ActivitySegmentWriter writer;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 보관_기간이_지난_구간만_지운다() {
		Long userId = newUser();
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String old = UUID.randomUUID().toString();
		String kept = UUID.randomUUID().toString();
		Instant oldEnd = now.minus(Duration.ofDays(187));
		Instant keptEnd = now.minus(Duration.ofDays(185));
		writer.upsert(userId, Map.of(
				old, new TimeRange(oldEnd.minusSeconds(60), oldEnd),
				kept, new TimeRange(keptEnd.minusSeconds(60), keptEnd)), now);

		job.run();

		assertThat(segmentIds(userId)).containsExactly(kept);
	}

	@Test
	void 많아도_나눠서_전부_지운다() {
		Long userId = newUser();
		Instant end = Instant.now().minus(Duration.ofDays(200)).truncatedTo(ChronoUnit.SECONDS);
		Map<String, TimeRange> segments = new HashMap<>();
		for (int i = 0; i < ActivityRetentionJob.BATCH_SIZE + 5; i++) {
			segments.put(UUID.randomUUID().toString(), new TimeRange(end.minusSeconds(60), end));
		}
		writer.upsert(userId, segments, Instant.now());

		int deleted = job.deleteSegmentsEndedBefore(Instant.now().minus(Duration.ofDays(186)));

		assertThat(deleted).isGreaterThanOrEqualTo(ActivityRetentionJob.BATCH_SIZE + 5);
		assertThat(segmentIds(userId)).isEmpty();
	}

	@Test
	void 보관_기간이_지난_하루_요약을_지운다() {
		Long userId = newUser();
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		insertDaily(userId, today.minusDays(190));
		insertDaily(userId, today.minusDays(100));

		job.run();

		assertThat(jdbcTemplate.queryForList("SELECT date FROM daily_activity WHERE user_id = ?", LocalDate.class, userId))
				.containsExactly(today.minusDays(100));
	}

	@Test
	void 여러_번_돌려도_결과가_같다() {
		Long userId = newUser();
		Instant end = Instant.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.SECONDS);
		String id = UUID.randomUUID().toString();
		writer.upsert(userId, Map.of(id, new TimeRange(end.minusSeconds(60), end)), Instant.now());

		job.run();
		job.run();

		assertThat(segmentIds(userId)).containsExactly(id);
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}

	private java.util.List<String> segmentIds(Long userId) {
		return jdbcTemplate.queryForList(
				"SELECT client_segment_id FROM activity_segment WHERE user_id = ?", String.class, userId);
	}

	private void insertDaily(Long userId, LocalDate date) {
		jdbcTemplate.update("INSERT INTO daily_activity (user_id, date, timezone, seconds, slots, computed_at)"
				+ " VALUES (?, ?, 'Asia/Seoul', 60, 0, ?)", userId, date, LocalDateTime.now(ZoneOffset.UTC));
	}
}
