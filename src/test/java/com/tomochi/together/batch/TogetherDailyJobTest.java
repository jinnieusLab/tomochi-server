package com.tomochi.together.batch;

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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class TogetherDailyJobTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

	@Autowired TogetherDailyJob job;
	@Autowired ActivitySegmentWriter writer;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired JdbcTemplate jdbcTemplate;

	private Long seoul;
	private Long la;
	/** 3일 전 UTC 05:00 = 서울 14시, LA 전날 22시(서머타임이면 21시~22시 사이) */
	private Instant base;

	@BeforeEach
	void setUp() {
		seoul = newUser("Asia/Seoul");
		la = newUser("America/Los_Angeles");
		befriend(seoul, la);
		base = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(3)).plus(Duration.ofHours(5));
	}

	@Test
	void 겹친_시간을_각자의_달력으로_한_줄씩_저장한다() {
		store(seoul, base, base.plus(Duration.ofHours(2)));
		store(la, base.plus(Duration.ofMinutes(30)), base.plus(Duration.ofHours(3)));

		job.aggregateAll();

		assertThat(seconds(seoul, la, LocalDate.ofInstant(base, SEOUL))).isEqualTo(5400);
		assertThat(seconds(la, seoul, LocalDate.ofInstant(base, LA))).isEqualTo(5400);
		assertThat(LocalDate.ofInstant(base, SEOUL)).isNotEqualTo(LocalDate.ofInstant(base, LA));
	}

	@Test
	void 다시_돌려도_결과가_같다() {
		store(seoul, base, base.plus(Duration.ofHours(1)));
		store(la, base, base.plus(Duration.ofHours(1)));

		job.aggregateAll();
		job.aggregateAll();

		assertThat(rowCount(seoul)).isEqualTo(1);
		assertThat(seconds(seoul, la, LocalDate.ofInstant(base, SEOUL))).isEqualTo(3600);
	}

	@Test
	void 늦게_올라온_구간도_다음_집계에_반영된다() {
		store(seoul, base, base.plus(Duration.ofHours(1)));
		store(la, base, base.plus(Duration.ofMinutes(30)));
		job.aggregateAll();

		// 며칠 뒤 LA 쪽 기기가 오프라인으로 쌓아 둔 구간을 올렸어요.
		store(la, base.plus(Duration.ofMinutes(30)), base.plus(Duration.ofHours(1)));
		job.aggregateAll();

		assertThat(seconds(seoul, la, LocalDate.ofInstant(base, SEOUL))).isEqualTo(3600);
	}

	@Test
	void 오늘처럼_아직_안_끝난_날은_저장하지_않는다() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		store(seoul, now.minusSeconds(600), now.minusSeconds(60));
		store(la, now.minusSeconds(600), now.minusSeconds(60));

		job.aggregateAll();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM together_daily WHERE owner_id = ? AND date = ?",
				Integer.class, seoul, LocalDate.now(SEOUL))).isZero();
	}

	@Test
	void 친구가_아니면_집계하지_않는다() {
		Long stranger = newUser("Asia/Seoul");
		store(seoul, base, base.plus(Duration.ofHours(1)));
		store(stranger, base, base.plus(Duration.ofHours(1)));

		job.aggregateAll();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM together_daily WHERE owner_id = ? AND friend_id = ?",
				Integer.class, seoul, stranger)).isZero();
	}

	private Long newUser(String timezone) {
		Long id = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		jdbcTemplate.update("UPDATE users SET timezone = ? WHERE id = ?", timezone, id);
		return id;
	}

	private void store(Long userId, Instant start, Instant end) {
		writer.upsert(userId, Map.of(UUID.randomUUID().toString(), new TimeRange(start, end)), Instant.now());
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}

	private Integer seconds(Long owner, Long friend, LocalDate date) {
		return jdbcTemplate.queryForObject(
				"SELECT COALESCE(SUM(seconds), 0) FROM together_daily WHERE owner_id = ? AND friend_id = ? AND date = ?",
				Integer.class, owner, friend, date);
	}

	private Integer rowCount(Long owner) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM together_daily WHERE owner_id = ?", Integer.class, owner);
	}
}
