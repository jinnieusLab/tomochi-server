package com.tomochi.friend.service;

import com.tomochi.friend.domain.Friendship;
import com.tomochi.friend.domain.FriendshipRepository;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 끊기. 양쪽 모두에서 관계가 사라지고, 상대에게는 알리지 않아요 (상대 목록에서 조용히 사라져요).
 * 둘 사이에만 의미가 있는 데이터(숨김 설정, 둘이 함께한 날, 아직 안 받은 꾹)도 같이 지워요.
 * 다시 친구가 되려면 초대 코드를 다시 받아야 해요.
 */
@Service
@RequiredArgsConstructor
public class UnfriendService {

	private final FriendshipRepository friendshipRepository;
	private final JdbcTemplate jdbcTemplate;

	@Transactional
	public void unfriend(Long userId, Long friendId) {
		Friendship friendship = friendshipRepository.findBetween(userId, friendId)
				.orElseThrow(() -> new BusinessException(FriendErrorCode.FRIEND_NOT_FOUND));
		jdbcTemplate.update("DELETE FROM friend_setting WHERE (owner_id = ? AND friend_id = ?) OR (owner_id = ? AND friend_id = ?)",
				userId, friendId, friendId, userId);
		// 둘이 함께한 날(TGTR-1)·안 받은 꾹(SQSH-1) 테이블이 생기면 여기서 같이 지워요.
		friendshipRepository.delete(friendship);
	}
}
