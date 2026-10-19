package com.tomochi.presence.service;

import com.tomochi.global.error.BusinessException;
import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.error.UserErrorCode;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상태 아이콘. 고른 값을 저장하고(다음에 앱을 켜도 남아요), 접속 중인 기기의 상태에도 바로 반영해요.
 * 아이콘을 언제 보여줄지(작업 중일 때만, 자리 비움이면 지우기)는 앱이 정하고 서버는 받은 값을 저장만 해요.
 */
@Service
@RequiredArgsConstructor
public class StatusIconService {

	private final UserRepository userRepository;
	private final PresenceStore presenceStore;
	private final PresenceNotifier presenceNotifier;
	private final Clock clock;

	@Transactional
	public void change(Long userId, StatusIcon icon) {
		User user = userRepository.findById(userId)
				.filter(found -> !found.isWithdrawn())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		user.changeStatusIcon(icon);
		Instant now = clock.instant();
		Presence before = presenceStore.find(userId, now);
		presenceStore.changeIcon(userId, icon);
		presenceNotifier.notifyIfChanged(userId, before, presenceStore.find(userId, now));
	}
}
