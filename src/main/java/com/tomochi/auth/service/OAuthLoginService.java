package com.tomochi.auth.service;

import com.tomochi.auth.domain.OAuthAccount;
import com.tomochi.auth.domain.OAuthAccountRepository;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.service.InviteCodeGenerator;
import com.tomochi.user.service.UserPurger;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 계정으로 사용자를 찾고, 없으면 만들어요.
 */
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

	private final OAuthAccountRepository oauthAccountRepository;
	private final UserRepository userRepository;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final UserPurger userPurger;
	private final Clock clock;

	/**
	 * 같은 소셜 계정으로 처음 로그인이 동시에 두 번 오면 둘 다 "없음"을 보고 만들려고 할 수 있어요.
	 * {@code (provider, provider_user_id)} 유니크 제약이 하나를 실패시키고,
	 * 실패한 쪽은 {@link OAuthLoginFacade}가 다시 찾아요.
	 *
	 * @return 사용자 id
	 */
	@Transactional
	public Long findOrSignUp(OAuthProvider provider, String providerUserId, PetKind petKind) {
		Optional<User> existing = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
				.map(OAuthAccount::getUser);
		if (existing.isEmpty()) {
			return signUp(provider, providerUserId, petKind);
		}
		User user = existing.get();
		if (user.isPastWithdrawalGrace(clock.instant())) {
			// 탈퇴하고 7일이 지났는데 정리 배치가 아직 안 돌았어요. 지금 지우고 새로 가입해요.
			userPurger.purge(user.getId());
			return signUp(provider, providerUserId, petKind);
		}
		// 7일 안에 다시 로그인하면 되살려요.
		user.restore();
		return user.getId();
	}

	@Transactional(readOnly = true)
	public Long find(OAuthProvider provider, String providerUserId) {
		return oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
				.map(account -> account.getUser().getId())
				.orElseThrow();
	}

	private Long signUp(OAuthProvider provider, String providerUserId, PetKind petKind) {
		User user = userRepository.save(User.signUp(petKind, inviteCodeGenerator.generateUnique()));
		oauthAccountRepository.saveAndFlush(new OAuthAccount(user, provider, providerUserId));
		return user.getId();
	}
}
