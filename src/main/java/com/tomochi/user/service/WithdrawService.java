package com.tomochi.user.service;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.error.UserErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. 바로 지우지 않고 표시만 해 두고, 7일 안에 다시 로그인하면 되살려요.
 * 모든 기기의 Refresh Token을 폐기해서 바로 로그아웃돼요. 이미 받은 Access Token으로는 내 정보를 볼 수 없어요.
 */
@Service
@RequiredArgsConstructor
public class WithdrawService {

	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
				.filter(found -> !found.isWithdrawn())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		Instant now = clock.instant();
		user.withdraw(now);
		jdbcTemplate.update("UPDATE refresh_token SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
				LocalDateTime.ofInstant(now, ZoneOffset.UTC), userId);
	}
}
