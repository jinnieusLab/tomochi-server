package com.tomochi.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.activity.dto.SegmentRequest;
import com.tomochi.activity.dto.UploadSegmentsRequest;
import com.tomochi.activity.service.ActivitySegmentService;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UploadSegmentsConcurrencyTest {

	private static final int THREADS = 10;

	@Autowired ActivitySegmentService activitySegmentService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 같은_id를_동시에_다른_범위로_보내도_행은_하나이고_합집합이_남는다() throws Exception {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		String id = UUID.randomUUID().toString();
		Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofHours(3));

		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
			for (int i = 0; i < THREADS; i++) {
				// 스레드마다 [base + i분, base + 60 + i분) : 가장 이른 시작 base, 가장 늦은 끝 base + 69분
				Instant s = base.plus(Duration.ofMinutes(i));
				Instant e = base.plus(Duration.ofMinutes(60 + i));
				results.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					return activitySegmentService.upload(userId,
							new UploadSegmentsRequest(List.of(new SegmentRequest(id, s, e))));
				}));
			}
			ready.await();
			start.countDown();
			for (Future<?> result : results) {
				result.get();
			}
		}

		List<Instant> row = jdbcTemplate.queryForObject(
				"SELECT started_at, ended_at FROM activity_segment WHERE user_id = ? AND client_segment_id = ?",
				(rs, n) -> List.of(rs.getTimestamp(1).toInstant(), rs.getTimestamp(2).toInstant()), userId, id);
		Integer rows = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM activity_segment WHERE user_id = ?", Integer.class, userId);

		assertThat(rows).isEqualTo(1);
		assertThat(row).containsExactly(base, base.plus(Duration.ofMinutes(69)));
	}
}
