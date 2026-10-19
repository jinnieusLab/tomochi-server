package com.tomochi.auth.service;

import com.tomochi.auth.config.AuthProperties;
import com.tomochi.auth.domain.RefreshToken;
import com.tomochi.auth.domain.RefreshTokenRepository;
import com.tomochi.auth.dto.TokenResponse;
import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 회전. 쓸 때마다 새 토큰을 주고 쓴 토큰은 폐기해요.
 * 폐기된 토큰이 다시 오면 누군가 훔쳐 쓴 것으로 보고 그 기기(family)의 토큰을 모두 끊어요.
 *
 * <pre>
 * 정상:      A ──회전──▶ B ──회전──▶ C
 * 탈취:      A ──(앱)──▶ B
 *            A ──(공격자, 폐기된 A 재사용)──▶ family 전체 폐기 → 앱도 다시 로그인
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenRotationService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final TokenService tokenService;
	private final AuthProperties authProperties;
	private final Clock clock;

	/**
	 * 재사용으로 family를 끊은 뒤 오류를 던져도 폐기는 남아야 해서, BusinessException에는 롤백하지 않아요.
	 * (이 메서드에서 BusinessException 전에 쓰는 건 회전 실패 쪽의 family 폐기뿐이에요.)
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public TokenResponse rotate(String rawRefreshToken) {
		Instant now = clock.instant();
		RefreshToken token = refreshTokenRepository.findByTokenHash(TokenService.hash(rawRefreshToken))
				.orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID));

		if (!token.getExpiresAt().isAfter(now)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}

		if (refreshTokenRepository.revokeIfActive(token.getId(), now) == 1) {
			return tokenService.issueInFamily(token.getUserId(), token.getFamilyId(), token.getDeviceName(), now);
		}
		throw handleRevokedTokenUse(token, now);
	}

	/**
	 * 이미 폐기된 토큰이 왔어요. 세 경우를 나눠요.
	 * <ul>
	 *   <li>family에 살아 있는 토큰이 없으면 로그아웃했거나 이미 끊긴 기기예요. 지킬 게 남지 않았으니 401로 다시 로그인하게 해요.</li>
	 *   <li>방금(유예 시간 안에) 회전으로 폐기됐으면 같은 토큰으로 동시에 보낸 요청이에요.
	 *       family는 그대로 두고 409로 알려요. 앱은 이긴 요청이 받은 새 토큰을 쓰면 돼요.</li>
	 *   <li>그 밖에는 재사용이에요. 탈취로 보고 family를 모두 끊고 403.</li>
	 * </ul>
	 */
	private BusinessException handleRevokedTokenUse(RefreshToken token, Instant now) {
		List<RefreshToken> family = refreshTokenRepository.findFamilyForUpdate(token.getFamilyId());
		RefreshToken current = family.stream()
				.filter(member -> member.getId().equals(token.getId()))
				.findFirst()
				.orElseThrow();
		boolean hasActiveSuccessor = family.stream().anyMatch(member -> member.getRevokedAt() == null);
		if (!hasActiveSuccessor) {
			return new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}

		boolean justRotated = current.getRevokedAt() != null
				&& !current.getRevokedAt().plus(authProperties.refreshReuseGrace()).isBefore(now);
		if (justRotated) {
			return new BusinessException(AuthErrorCode.REFRESH_TOKEN_ROTATED);
		}

		int revoked = refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
		log.warn("폐기된 Refresh Token 재사용. userId={}, familyId={}, 함께 폐기한 토큰 {}개",
				token.getUserId(), token.getFamilyId(), revoked);
		return new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
	}
}
