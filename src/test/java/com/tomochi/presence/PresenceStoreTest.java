package com.tomochi.presence;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.presence.service.PresenceStore;
import com.tomochi.support.IntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@IntegrationTest
class PresenceStoreTest {

	@Autowired PresenceStore store;
	@Autowired StringRedisTemplate redisTemplate;

	private final Long userId = ThreadLocalRandom.current().nextLong(1_000_000, Long.MAX_VALUE);
	private final Instant t0 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

	@Test
	void 작업_중으로_바뀐_순간을_기억하고_계속_작업_중이면_그대로다() {
		assertThat(store.update(userId, "mac", PresenceStatus.WORKING, StatusIcon.STUDY, t0)).isEqualTo(t0);
		assertThat(store.update(userId, "mac", PresenceStatus.WORKING, StatusIcon.CODE, t0.plusSeconds(30))).isEqualTo(t0);

		Presence presence = store.find(userId, t0.plusSeconds(30));
		assertThat(presence.status()).isEqualTo(PresenceStatus.WORKING);
		assertThat(presence.workingSince()).isEqualTo(t0);
		assertThat(presence.icon()).isEqualTo(StatusIcon.CODE);
	}

	@Test
	void 쉬었다가_다시_작업하면_시작_시각이_새로_정해진다() {
		store.update(userId, "mac", PresenceStatus.WORKING, null, t0);
		assertThat(store.update(userId, "mac", PresenceStatus.RESTING, null, t0.plusSeconds(120))).isNull();

		Instant again = t0.plusSeconds(300);
		assertThat(store.update(userId, "mac", PresenceStatus.WORKING, null, again)).isEqualTo(again);
	}

	@Test
	void 기기가_여러_대면_하나라도_작업_중이면_작업_중이다() {
		store.update(userId, "macbook", PresenceStatus.RESTING, null, t0);
		store.update(userId, "imac", PresenceStatus.WORKING, StatusIcon.DESIGN, t0);

		assertThat(store.find(userId, t0).status()).isEqualTo(PresenceStatus.WORKING);
	}

	@Test
	void 기기마다_90초_TTL이_걸린다() {
		store.update(userId, "mac", PresenceStatus.WORKING, null, t0);

		List<Long> ttl = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<List<Long>>) connection ->
				connection.hashCommands().hTtl(("presence:" + userId).getBytes(), "mac".getBytes()));
		assertThat(ttl).hasSize(1);
		assertThat(ttl.getFirst()).isBetween(80L, 90L);
	}

	@Test
	void 하트비트가_90초_넘게_끊긴_기기는_오프라인이다() {
		store.update(userId, "mac", PresenceStatus.WORKING, null, t0);

		assertThat(store.find(userId, t0.plusSeconds(91)).status()).isEqualTo(PresenceStatus.OFFLINE);
	}

	@Test
	void 여러_사람을_한_번에_읽고_없는_사람은_오프라인이다() {
		Long other = userId + 1;
		store.update(userId, "mac", PresenceStatus.AWAY, null, t0);

		Map<Long, Presence> presences = store.findAll(List.of(userId, other), t0);

		assertThat(presences.get(userId).status()).isEqualTo(PresenceStatus.AWAY);
		assertThat(presences.get(other)).isEqualTo(Presence.OFFLINE);
	}
}
