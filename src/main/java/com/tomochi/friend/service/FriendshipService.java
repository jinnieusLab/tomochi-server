package com.tomochi.friend.service;

import com.tomochi.friend.domain.Friendship;
import com.tomochi.friend.domain.FriendshipRepository;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.global.error.BusinessException;
import com.tomochi.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 맺기 트랜잭션. 두 가지 경쟁을 막아요.
 *
 * <ul>
 *   <li><b>20명 상한</b>: 두 사람이 동시에 나를 추가하면 둘 다 "지금 19명"을 보고 21명이 될 수 있어요.
 *       두 사용자 행을 id 순서로 잠근 뒤에 세서, 나를 포함한 친구 맺기는 한 번에 하나씩만 진행돼요.</li>
 *   <li><b>중복 관계</b>: A가 B의 코드를, B가 A의 코드를 동시에 넣어도 같은 두 행을 같은 순서로 잠그니 차례로 진행되고,
 *       뒤의 요청은 이미 있는 관계를 봐요. 그래도 남는 경우는 유니크 제약이 막아요({@link FriendshipFacade}).</li>
 * </ul>
 *
 * <p>MySQL 기본 격리 수준(REPEATABLE READ)은 트랜잭션의 첫 일반 SELECT 때 스냅샷을 정해요. 잠금보다 먼저 SELECT하면
 * 잠금을 기다리는 동안 상대가 만든 관계가 그 뒤 SELECT에 안 보여요. 그래서 이 트랜잭션은 <b>잠금부터</b> 시작하고,
 * 상대 찾기(초대 코드 조회)는 트랜잭션 밖에서 해요.
 */
@Service
@RequiredArgsConstructor
public class FriendshipService {

	public static final int MAX_FRIENDS = 20;

	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;

	/** @return 새로 맺었으면 true, 이미 친구였으면 false */
	@Transactional
	public boolean befriend(Long userId, Long targetId) {
		// 첫 문장이 잠금이라, 이 뒤의 SELECT는 잠금을 얻은 뒤의 최신 커밋을 봐요.
		if (userRepository.lockInIdOrder(userId, targetId).size() != 2) {
			throw new BusinessException(FriendErrorCode.INVITE_CODE_NOT_FOUND);
		}
		if (friendshipRepository.findBetween(userId, targetId).isPresent()) {
			return false;
		}
		if (friendshipRepository.countByUser(userId) >= MAX_FRIENDS
				|| friendshipRepository.countByUser(targetId) >= MAX_FRIENDS) {
			throw new BusinessException(FriendErrorCode.FRIEND_LIMIT_EXCEEDED);
		}
		friendshipRepository.saveAndFlush(Friendship.between(userId, targetId));
		return true;
	}
}
