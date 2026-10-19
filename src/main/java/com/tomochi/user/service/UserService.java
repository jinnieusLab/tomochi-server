package com.tomochi.user.service;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.domain.Nickname;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.dto.UserResponse;
import com.tomochi.user.dto.UpdateUserRequest;
import com.tomochi.user.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserResponse findUser(Long userId) {
		return UserResponse.from(getUser(userId));
	}

	/** 보낸 필드만 바꿔요. 보내지 않은 필드는 그대로예요. */
	@Transactional
	public UserResponse updateUser(Long userId, UpdateUserRequest request) {
		User user = getUser(userId);
		if (request.hasNickname()) {
			user.changeNickname(Nickname.normalize(request.nickname()));
		}
		if (request.hasPetKind()) {
			user.changePetKind(PetKind.from(request.petKind()));
		}
		return UserResponse.from(user);
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}
}
