package com.tomochi.squish.service;

import com.tomochi.squish.dto.PendingSquishesResponse;
import com.tomochi.squish.dto.PendingSquishesResponse.Item;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 앱이 꺼져 있던 동안의 꾹. 몇 개 잃어버려도 큰일은 아니지만, 켰을 때 "누가 찾아왔었네"는 남겨 주고 싶어서
 * DB에 두되 30일만 들고 있어요. 친구별로 묶어 한 번에 알려요.
 */
@Service
@RequiredArgsConstructor
public class PendingSquishService {

	public static final Duration KEEP = Duration.ofDays(30);

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public PendingSquishesResponse findPending(Long userId) {
		LocalDateTime since = LocalDateTime.ofInstant(clock.instant().minus(KEEP), ZoneOffset.UTC);
		Map<Long, List<Long>> idsByFriend = new LinkedHashMap<>();
		Map<Long, Instant> lastSentByFriend = new LinkedHashMap<>();
		jdbcTemplate.query("""
						SELECT s.id, s.from_user_id, s.sent_at FROM squish s
						JOIN users u ON u.id = s.from_user_id AND u.deleted_at IS NULL
						WHERE s.to_user_id = ? AND s.delivered_at IS NULL AND s.sent_at >= ?
						ORDER BY s.sent_at
						""",
				rs -> {
					long from = rs.getLong("from_user_id");
					idsByFriend.computeIfAbsent(from, id -> new ArrayList<>()).add(rs.getLong("id"));
					lastSentByFriend.put(from, rs.getObject("sent_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
				},
				userId, since);

		List<Item> items = new ArrayList<>();
		idsByFriend.forEach((from, ids) -> items.add(new Item(from, ids.size(), lastSentByFriend.get(from), ids)));
		items.sort(Comparator.comparing(Item::lastSentAt).reversed());
		return new PendingSquishesResponse(items);
	}
}
