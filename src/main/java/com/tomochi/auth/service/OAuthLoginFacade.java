package com.tomochi.auth.service;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.user.domain.PetKind;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 첫 로그인이 동시에 와서 유니크 제약에 걸리면, 먼저 만든 쪽 계정을 다시 찾아 돌려줘요.
 * 실패한 트랜잭션은 이미 롤백됐으니 새 트랜잭션에서 찾아야 해서 서비스 밖에 둬요.
 */
@Component
@RequiredArgsConstructor
public class OAuthLoginFacade {

	private final OAuthLoginService oauthLoginService;

	public Long login(OAuthProvider provider, String providerUserId, PetKind petKind) {
		try {
			return oauthLoginService.findOrSignUp(provider, providerUserId, petKind);
		} catch (DataIntegrityViolationException e) {
			return oauthLoginService.find(provider, providerUserId);
		}
	}
}
