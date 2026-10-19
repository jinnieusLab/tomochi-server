package com.tomochi.friend.service;

import com.tomochi.friend.dto.FriendResponse;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.error.BusinessException;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 초대 코드로 친구 맺기. 상대 찾기는 트랜잭션 밖에서 하고, 맺기는 잠금으로 시작하는 트랜잭션에서 해요
 * (이유는 {@link FriendshipService}).
 */
@Component
@RequiredArgsConstructor
public class FriendshipFacade {

	private final UserRepository userRepository;
	private final FriendshipService friendshipService;

	public FriendResponse addByInviteCode(Long userId, String rawInviteCode) {
		User target = userRepository.findByInviteCodeAndDeletedAtIsNull(rawInviteCode.strip().toUpperCase(Locale.ROOT))
				.orElseThrow(() -> new BusinessException(FriendErrorCode.INVITE_CODE_NOT_FOUND));
		if (target.getId().equals(userId)) {
			throw new BusinessException(FriendErrorCode.OWN_INVITE_CODE);
		}
		try {
			friendshipService.befriend(userId, target.getId());
		} catch (DataIntegrityViolationException e) {
			// 잠금으로도 남는 드문 경쟁에서 같은 관계가 먼저 만들어졌어요. 이미 친구예요.
		}
		return FriendResponse.from(target);
	}
}
