package com.tomochi.auth.service;

import com.tomochi.auth.config.AuthProperties;
import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.global.error.BusinessException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 로그인 직후 앱에 넘기는 1회용 코드.
 * 커스텀 URL({@code tomochi://...})은 같은 스킴을 등록한 다른 앱이 가로챌 수 있어서
 * URL에는 토큰 대신 이 코드만 실어요. 코드는 60초 동안 한 번만 쓸 수 있고, verifier 없이는 쓸 수 없어요.
 */
@Service
@RequiredArgsConstructor
public class AuthCodeService {

	private static final String KEY_PREFIX = "auth:code:";
	private static final int CODE_BYTES = 32;

	private final SecureRandom random = new SecureRandom();
	private final StringRedisTemplate redisTemplate;
	private final AuthProperties authProperties;

	public String issue(Long userId, String challenge) {
		String code = randomCode();
		redisTemplate.opsForValue().set(KEY_PREFIX + code, userId + ":" + challenge, authProperties.authCodeTtl());
		return code;
	}

	/**
	 * 코드를 꺼내면서 바로 지워요 (GETDEL 한 번으로 원자적).
	 * 같은 코드로 동시에 두 요청이 와도 하나만 값을 받아요.
	 * verifier가 틀려도 코드는 이미 지워져서, 같은 코드로 verifier를 여러 번 맞혀 볼 수 없어요.
	 *
	 * @return 로그인한 사용자 id
	 */
	public Long consume(String code, String codeVerifier) {
		String value = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
		if (value == null) {
			throw new BusinessException(AuthErrorCode.AUTH_CODE_INVALID);
		}
		int separator = value.indexOf(':');
		Long userId = Long.valueOf(value.substring(0, separator));
		String challenge = value.substring(separator + 1);
		if (!Pkce.matches(codeVerifier, challenge)) {
			throw new BusinessException(AuthErrorCode.AUTH_CODE_INVALID);
		}
		return userId;
	}

	private String randomCode() {
		byte[] bytes = new byte[CODE_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
