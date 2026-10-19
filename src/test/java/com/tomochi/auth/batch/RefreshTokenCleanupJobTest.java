package com.tomochi.auth.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class RefreshTokenCleanupJobTest {

	@Autowired RefreshTokenCleanupJob job;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void 끝난_지_7일이_지난_토큰만_지운다() {
		Long userId = oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
		for (int i = 0; i < 5; i++) {
			tokenService.issueForNewLogin(userId, "기기" + i);
		}
		Long[] ids = jdbcTemplate.queryForList("SELECT id FROM refresh_token WHERE user_id = ? ORDER BY id", Long.class,
				userId).toArray(Long[]::new);
		// 0: 살아 있음, 1: 8일 전 만료, 2: 3일 전 만료, 3: 8일 전 폐기, 4: 1일 전 폐기
		jdbcTemplate.update("UPDATE refresh_token SET expires_at = UTC_TIMESTAMP(6) - INTERVAL 8 DAY WHERE id = ?", ids[1]);
		jdbcTemplate.update("UPDATE refresh_token SET expires_at = UTC_TIMESTAMP(6) - INTERVAL 3 DAY WHERE id = ?", ids[2]);
		jdbcTemplate.update("UPDATE refresh_token SET revoked_at = UTC_TIMESTAMP(6) - INTERVAL 8 DAY WHERE id = ?", ids[3]);
		jdbcTemplate.update("UPDATE refresh_token SET revoked_at = UTC_TIMESTAMP(6) - INTERVAL 1 DAY WHERE id = ?", ids[4]);

		job.run();

		assertThat(jdbcTemplate.queryForList("SELECT id FROM refresh_token WHERE user_id = ? ORDER BY id", Long.class,
				userId)).containsExactly(ids[0], ids[2], ids[4]);
	}
}
