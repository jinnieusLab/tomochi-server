package com.tomochi.squish.service;

import com.tomochi.realtime.UserMessageSender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 꾹 전달. 상대가 연결돼 있으면 {@code squish.received}를 바로 보내고, 앱이 {@code squish.ack}로 받았다고 알리면
 * 전달 완료로 표시해요. 연결이 없거나 ack가 오지 않은 꾹은 다음에 연결될 때 모아서 보내요 (SQSH-3).
 * 보낸 시각부터 ack까지를 {@code tomochi.squish.delivery} 지표로 남겨요 (목표 p95 1초).
 */
@Component
public class SquishDelivery {

	private final UserMessageSender sender;
	private final NamedParameterJdbcTemplate jdbc;
	private final Clock clock;
	private final Timer deliveryTimer;

	public SquishDelivery(UserMessageSender sender, JdbcTemplate jdbcTemplate, Clock clock, MeterRegistry meterRegistry) {
		this.sender = sender;
		this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
		this.clock = clock;
		this.deliveryTimer = Timer.builder("tomochi.squish.delivery")
				.description("꾹을 보낸 시각부터 상대 앱이 받았다고 알린 시각까지")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(meterRegistry);
	}

	public void push(Long squishId, Long fromUserId, Long toUserId, Instant sentAt) {
		sender.send(toUserId, "squish.received",
				Map.of("squishId", squishId, "fromFriendId", fromUserId, "sentAt", sentAt.toString()));
	}

	/**
	 * 받은 사람 자신의 꾹만 표시해요. 남의 꾹 id를 보내도 바뀌지 않아요. 여러 번 ack해도 처음 시각이 남아요.
	 *
	 * @return 새로 전달 완료로 표시한 수
	 */
	@Transactional
	public int acknowledge(Long userId, List<Long> squishIds) {
		if (squishIds == null || squishIds.isEmpty()) {
			return 0;
		}
		Instant now = clock.instant();
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("ids", squishIds)
				.addValue("now", LocalDateTime.ofInstant(now, ZoneOffset.UTC));
		List<LocalDateTime> sentTimes = jdbc.queryForList("""
				SELECT sent_at FROM squish WHERE to_user_id = :userId AND id IN (:ids) AND delivered_at IS NULL
				""", params, LocalDateTime.class);
		int updated = jdbc.update("""
				UPDATE squish SET delivered_at = :now WHERE to_user_id = :userId AND id IN (:ids) AND delivered_at IS NULL
				""", params);
		for (LocalDateTime sentAt : sentTimes) {
			deliveryTimer.record(Duration.between(sentAt.toInstant(ZoneOffset.UTC), now));
		}
		return updated;
	}
}
