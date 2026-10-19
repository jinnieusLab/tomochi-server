package com.tomochi.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class OAuthLoginConcurrencyTest {

	private static final int THREADS = 8;

	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 같은_소셜_계정으로_첫_로그인이_동시에_와도_계정은_하나다() throws Exception {
		String providerUserId = UUID.randomUUID().toString();
		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Long>> results = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
			for (int i = 0; i < THREADS; i++) {
				results.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					return oauthLoginFacade.login(OAuthProvider.GITHUB, providerUserId, PetKind.CHOCO);
				}));
			}
			ready.await();
			start.countDown();

			List<Long> userIds = new ArrayList<>();
			for (Future<Long> result : results) {
				userIds.add(result.get());
			}

			assertThat(userIds).containsOnly(userIds.getFirst());
		}

		Integer accounts = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM oauth_account WHERE provider = 'GITHUB' AND provider_user_id = ?",
				Integer.class, providerUserId);
		assertThat(accounts).isEqualTo(1);
	}
}
