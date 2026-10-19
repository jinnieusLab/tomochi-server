package com.tomochi.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.error.BusinessException;
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
class RefreshTokenConcurrencyTest {

	private static final int THREADS = 10;

	@Autowired RefreshTokenRotationService rotationService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 같은_토큰으로_동시에_재발급하면_하나만_성공하고_family는_살아_있다() throws Exception {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		String refreshToken = tokenService.issueForNewLogin(userId, "내 맥북").refreshToken();
		String familyId = jdbcTemplate.queryForObject("SELECT family_id FROM refresh_token WHERE user_id = ?",
				String.class, userId);

		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Object>> results = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
			for (int i = 0; i < THREADS; i++) {
				results.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					try {
						return rotationService.rotate(refreshToken);
					} catch (BusinessException e) {
						return e.getErrorCode();
					}
				}));
			}
			ready.await();
			start.countDown();

			int succeeded = 0;
			int rotatedByOther = 0;
			for (Future<Object> result : results) {
				Object outcome = result.get();
				if (outcome instanceof TokenResponse) {
					succeeded++;
				} else if (outcome == AuthErrorCode.REFRESH_TOKEN_ROTATED) {
					rotatedByOther++;
				}
			}

			assertThat(succeeded).isEqualTo(1);
			assertThat(rotatedByOther).isEqualTo(THREADS - 1);
		}

		Integer active = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM refresh_token WHERE family_id = ? AND revoked_at IS NULL", Integer.class, familyId);
		Integer total = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM refresh_token WHERE family_id = ?", Integer.class, familyId);
		assertThat(active).isEqualTo(1);
		assertThat(total).isEqualTo(2);
	}
}
