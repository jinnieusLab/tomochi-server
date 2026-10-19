package com.tomochi.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴한 사용자의 데이터를 모두 지워요. 외래 키 순서대로 자식 테이블부터 지워요.
 * 새 테이블(친구 관계, 꾹, 둘이 함께한 날)이 생기면 여기에 더해요.
 */
@Component
@RequiredArgsConstructor
public class UserPurger {

	private static final String[] CHILD_TABLES = {
			"daily_activity",
			"activity_segment",
			"refresh_token",
			"oauth_account",
	};

	private final JdbcTemplate jdbcTemplate;

	/** 사용자 한 명을 한 트랜잭션에서 지워요. 실패하면 그 사람만 롤백되고 다음 배치에서 다시 시도해요. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void purge(Long userId) {
		for (String table : CHILD_TABLES) {
			jdbcTemplate.update("DELETE FROM " + table + " WHERE user_id = ?", userId);
		}
		jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
	}
}
