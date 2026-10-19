package com.tomochi.friend.service;

import com.tomochi.friend.domain.FriendshipRepository;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.error.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 목록에서 숨기기. 내 쪽에서만 숨기고 상대는 몰라요. 친구 관계와 둘이 함께한 날은 그대로 쌓여요.
 * 숨긴 친구가 보낸 꾹과 작업 시작 알림(친구 펫 등장)은 받지 않아요 ({@link #isHidden}로 확인).
 */
@Service
@RequiredArgsConstructor
public class FriendSettingService {

	private final FriendshipRepository friendshipRepository;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	@Transactional
	public void setHidden(Long ownerId, Long friendId, boolean hidden) {
		if (friendshipRepository.findBetween(ownerId, friendId).isEmpty()) {
			throw new BusinessException(FriendErrorCode.FRIEND_NOT_FOUND);
		}
		jdbcTemplate.update("""
						INSERT INTO friend_setting (owner_id, friend_id, hidden, updated_at) VALUES (?, ?, ?, ?) AS incoming
						ON DUPLICATE KEY UPDATE hidden = incoming.hidden, updated_at = incoming.updated_at
						""",
				ownerId, friendId, hidden, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
	}

	/** owner가 friend를 숨겼는지. 꾹·작업 시작 알림을 보내기 전에 확인해요. */
	public boolean isHidden(Long ownerId, Long friendId) {
		Boolean hidden = jdbcTemplate.query("SELECT hidden FROM friend_setting WHERE owner_id = ? AND friend_id = ?",
				rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE, ownerId, friendId);
		return Boolean.TRUE.equals(hidden);
	}
}
