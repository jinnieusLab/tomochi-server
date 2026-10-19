package com.tomochi.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 같은 사용자의 서로 다른 필드 변경이 겹쳐도 둘 다 남는지 확인해요.
 * 순서를 고정하려고 래치로 끼워 넣어요:
 * <pre>
 * A: 읽기 ── 닉네임 바꿈 ─────────────(대기)───────────── 커밋
 * B:            읽기 ── 펫 바꿈 ── 커밋
 * </pre>
 */
@IntegrationTest
class ConcurrentUserUpdateTest {

	private static final long TIMEOUT_SECONDS = 10;

	@Autowired UserRepository userRepository;
	@Autowired TransactionTemplate transactionTemplate;
	@Autowired OAuthLoginFacade oauthLoginFacade;

	@Test
	void 닉네임과_펫을_동시에_바꿔도_둘_다_남는다() throws Exception {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		CountDownLatch aRead = new CountDownLatch(1);
		CountDownLatch bCommitted = new CountDownLatch(1);

		CompletableFuture<Void> a = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
			User user = userRepository.findById(userId).orElseThrow();
			user.changeNickname("민지");
			aRead.countDown();
			await(bCommitted);
		}));

		await(aRead);
		try {
			transactionTemplate.executeWithoutResult(status ->
					userRepository.findById(userId).orElseThrow().changePetKind(PetKind.BUTTER));
		} finally {
			// B가 실패해도 A를 풀어 줘요. 안 그러면 A가 끝없이 기다려요.
			bCommitted.countDown();
		}
		a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getNickname()).isEqualTo("민지");
		assertThat(user.getPetKind()).isEqualTo(PetKind.BUTTER);
	}

	/** A가 래치 전에 실패해도 테스트가 멈추지 않게 제한 시간을 둬요. */
	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException(TIMEOUT_SECONDS + "초 안에 상대 트랜잭션이 진행되지 않았어요");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
