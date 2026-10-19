package com.tomochi.friend.service;

import com.tomochi.friend.dto.InviteCodeResponse;
import com.tomochi.global.error.BusinessException;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.error.UserErrorCode;
import com.tomochi.user.service.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 초대 코드 재발급. 새 코드로 바꾸는 순간 이전 코드로는 친구를 맺을 수 없어요.
 *
 * <p>"안 쓰이는 코드 뽑기"와 "저장" 사이에 다른 사람이 같은 코드를 가져가면 유니크 제약에 걸려요.
 * 그 트랜잭션은 롤백됐으니 새 트랜잭션에서 다시 뽑아요. 31^6 ≈ 8억 8천만 가지라 거의 일어나지 않아요.
 */
@Service
@RequiredArgsConstructor
public class InviteCodeService {

	private static final int MAX_ATTEMPTS = 3;

	private final UserRepository userRepository;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final TransactionTemplate transactionTemplate;

	public InviteCodeResponse regenerate(Long userId) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			String code = inviteCodeGenerator.generateUnique();
			try {
				transactionTemplate.executeWithoutResult(status -> {
					User user = userRepository.findById(userId)
							.filter(found -> !found.isWithdrawn())
							.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
					user.changeInviteCode(code);
					userRepository.flush();
				});
				return InviteCodeResponse.of(code);
			} catch (DataIntegrityViolationException e) {
				// 다른 사람이 방금 같은 코드를 가져갔어요. 다시 뽑아요.
			}
		}
		throw new IllegalStateException("초대 코드를 " + MAX_ATTEMPTS + "번 만에 만들지 못했어요");
	}
}
