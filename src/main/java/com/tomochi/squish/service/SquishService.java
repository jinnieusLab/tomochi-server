package com.tomochi.squish.service;

import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.friend.repository.FriendQuery;
import com.tomochi.friend.service.FriendSettingService;
import com.tomochi.global.error.BusinessException;
import com.tomochi.squish.error.SquishErrorCode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/**
 * 꾹 보내기. 친구 화면의 펫이 소리 없이 출렁이고 로컬 알림이 떠요 (실시간 전달은 SQSH-2, 꺼져 있던 동안은 SQSH-3).
 * 쉬는 중·자리 비움·오프라인인 친구에게도 보낼 수 있어요.
 */
@Service
@RequiredArgsConstructor
public class SquishService {

	private final FriendQuery friendQuery;
	private final FriendSettingService friendSettingService;
	private final SquishCooldown cooldown;
	private final JdbcTemplate jdbcTemplate;
	private final SquishDelivery squishDelivery;
	private final Clock clock;

	public void send(Long fromUserId, Long toUserId) {
		if (friendQuery.findFriend(fromUserId, toUserId).isEmpty()) {
			throw new BusinessException(FriendErrorCode.FRIEND_NOT_FOUND);
		}
		if (!cooldown.tryStart(fromUserId, toUserId)) {
			throw new BusinessException(SquishErrorCode.SQUISH_COOLDOWN, cooldown.remainingSeconds(fromUserId, toUserId));
		}
		// 상대가 나를 숨겼으면 성공처럼 답하고 전달하지 않아요. 숨긴 걸 눈치채지 못하게요.
		if (friendSettingService.isHidden(toUserId, fromUserId)) {
			return;
		}
		Instant sentAt = clock.instant();
		GeneratedKeyHolder key = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO squish (from_user_id, to_user_id, sent_at) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, fromUserId);
			statement.setLong(2, toUserId);
			statement.setObject(3, LocalDateTime.ofInstant(sentAt, ZoneOffset.UTC));
			return statement;
		}, key);
		squishDelivery.push(key.getKey().longValue(), fromUserId, toUserId, sentAt);
	}
}
