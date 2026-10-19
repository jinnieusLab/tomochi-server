package com.tomochi.user.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴한 사용자의 데이터를 모두 지워요. 외래 키 순서대로 자식 테이블부터 지워요.
 * 새 테이블(꾹, 둘이 함께한 날)이 생기면 여기에 더해요.
 */
@Component
@RequiredArgsConstructor
public class UserPurger {

	/** 사용자 id 하나를 받는 DELETE 문. 친구 관계는 상대 쪽에서도 사라져요. */
	private static final List<String> DELETES = List.of(
			"DELETE FROM together_daily WHERE owner_id = ?1 OR friend_id = ?1",
			"DELETE FROM squish WHERE from_user_id = ?1 OR to_user_id = ?1",
			"DELETE FROM friend_setting WHERE owner_id = ?1 OR friend_id = ?1",
			"DELETE FROM friendship WHERE user_low = ?1 OR user_high = ?1",
			"DELETE FROM daily_activity WHERE user_id = ?1",
			"DELETE FROM activity_segment WHERE user_id = ?1",
			"DELETE FROM refresh_token WHERE user_id = ?1",
			"DELETE FROM oauth_account WHERE user_id = ?1",
			"DELETE FROM users WHERE id = ?1");

	private final JdbcTemplate jdbcTemplate;

	/** 사용자 한 명을 한 트랜잭션에서 지워요. 실패하면 그 사람만 롤백되고 다음 배치에서 다시 시도해요. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void purge(Long userId) {
		for (String delete : DELETES) {
			String sql = delete.replace("?1", "?");
			Object[] args = new Object[countPlaceholders(sql)];
			java.util.Arrays.fill(args, userId);
			jdbcTemplate.update(sql, args);
		}
	}

	private static int countPlaceholders(String sql) {
		return (int) sql.chars().filter(c -> c == '?').count();
	}
}
