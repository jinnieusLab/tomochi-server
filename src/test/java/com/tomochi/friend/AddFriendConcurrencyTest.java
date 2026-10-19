package com.tomochi.friend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.friend.service.FriendshipFacade;
import com.tomochi.global.error.BusinessException;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class AddFriendConcurrencyTest {

	@Autowired FriendshipFacade friendshipFacade;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired UserRepository userRepository;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 친구가_18명인_사람을_6명이_동시에_추가하면_2명만_된다() throws Exception {
		Long popular = newUser();
		for (int i = 0; i < 18; i++) {
			insertFriendship(popular, newUser());
		}
		String code = userRepository.findById(popular).orElseThrow().getInviteCode();
		List<Long> newcomers = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			newcomers.add(newUser());
		}

		List<Object> outcomes = runAtOnce(newcomers.stream()
				.<Callable<Object>>map(id -> () -> friendshipFacade.addByInviteCode(id, code))
				.toList());

		assertThat(outcomes.stream().filter(o -> !(o instanceof FriendErrorCode)).count()).isEqualTo(2);
		assertThat(outcomes).filteredOn(o -> o == FriendErrorCode.FRIEND_LIMIT_EXCEEDED).hasSize(4);
		assertThat(friendCount(popular)).isEqualTo(20);
	}

	@Test
	void 서로의_코드를_동시에_넣어도_관계는_하나다() throws Exception {
		for (int round = 0; round < 10; round++) {
			Long a = newUser();
			Long b = newUser();
			String codeA = userRepository.findById(a).orElseThrow().getInviteCode();
			String codeB = userRepository.findById(b).orElseThrow().getInviteCode();

			List<Object> outcomes = runAtOnce(List.of(
					() -> friendshipFacade.addByInviteCode(a, codeB),
					() -> friendshipFacade.addByInviteCode(b, codeA)));

			assertThat(outcomes).noneMatch(o -> o instanceof FriendErrorCode);
			assertThat(jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM friendship WHERE user_low = ? AND user_high = ?",
					Integer.class, Math.min(a, b), Math.max(a, b))).isEqualTo(1);
		}
	}

	private List<Object> runAtOnce(List<Callable<Object>> tasks) throws Exception {
		CountDownLatch ready = new CountDownLatch(tasks.size());
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Object>> futures = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
			for (Callable<Object> task : tasks) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					try {
						return task.call();
					} catch (BusinessException e) {
						return e.getErrorCode();
					}
				}));
			}
			ready.await();
			start.countDown();
			List<Object> outcomes = new ArrayList<>();
			for (Future<Object> future : futures) {
				outcomes.add(future.get());
			}
			return outcomes;
		}
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}

	private void insertFriendship(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}

	private Integer friendCount(Long userId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friendship WHERE user_low = ? OR user_high = ?",
				Integer.class, userId, userId);
	}
}
