package com.tomochi.auth.service;

import com.tomochi.auth.domain.RefreshToken;
import com.tomochi.auth.domain.RefreshTokenRepository;
import com.tomochi.auth.domain.RefreshTokenRepository.FamilySignIn;
import com.tomochi.auth.dto.SessionResponse;
import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.error.BusinessException;
import com.tomochi.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기기별 세션. 세션 하나 = Refresh Token family 하나예요.
 * 로그아웃은 family를 폐기하는 것뿐이고, 이미 발급된 Access Token은 블랙리스트 없이 만료(최대 30분)까지 둬요.
 * 대신 Access Token을 짧게 두고, 다음 재발급부터는 막혀요.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public List<SessionResponse> findSessions(AuthUser user) {
		List<RefreshToken> active = refreshTokenRepository.findActiveByUserId(user.userId(), clock.instant());
		if (active.isEmpty()) {
			return List.of();
		}
		Map<String, Instant> signedInAt = refreshTokenRepository
				.findSignedInAt(active.stream().map(RefreshToken::getFamilyId).toList())
				.stream()
				.collect(Collectors.toMap(FamilySignIn::getFamilyId, FamilySignIn::getSignedInAt));

		return active.stream()
				.map(token -> new SessionResponse(
						token.getFamilyId(),
						token.getDeviceName(),
						signedInAt.get(token.getFamilyId()),
						token.getCreatedAt(),
						token.getFamilyId().equals(user.sessionId())))
				.toList();
	}

	/** 지금 기기 로그아웃. 여러 번 불러도 결과는 같아요. */
	@Transactional
	public void logout(AuthUser user) {
		refreshTokenRepository.revokeFamily(user.sessionId(), clock.instant());
	}

	/**
	 * 다른 기기(또는 지금 기기) 로그아웃. 남의 세션이면 있는지조차 알려주지 않고 404예요.
	 * 이미 끊긴 내 세션이면 그대로 성공해요.
	 */
	@Transactional
	public void revoke(AuthUser user, String sessionId) {
		if (!refreshTokenRepository.existsByFamilyIdAndUserId(sessionId, user.userId())) {
			throw new BusinessException(AuthErrorCode.SESSION_NOT_FOUND);
		}
		refreshTokenRepository.revokeFamily(sessionId, clock.instant());
	}
}
