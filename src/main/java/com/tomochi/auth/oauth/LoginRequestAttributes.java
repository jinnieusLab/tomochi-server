package com.tomochi.auth.oauth;

import com.tomochi.auth.service.Pkce;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * 앱이 로그인 시작 주소에 붙인 값. 로그인 요청에 담겨 Redis에 저장되고, 제공자에서 돌아왔을 때 다시 꺼내요.
 */
public final class LoginRequestAttributes {

	public static final String AUTHORIZATION_BASE_URI = "/api/v1/auth/oauth2";
	public static final String CALLBACK_URI = "/api/v1/auth/oauth2/callback/*";

	public static final String CHALLENGE = "challenge";
	public static final String PET_KIND = "petKind";

	private LoginRequestAttributes() {
	}

	public static String challenge(OAuth2AuthorizationRequest request) {
		String challenge = request.getAttribute(CHALLENGE);
		if (!Pkce.isValidChallenge(challenge)) {
			throw new IllegalStateException("challenge 없이 시작된 로그인이에요");
		}
		return challenge;
	}

	public static String petKind(OAuth2AuthorizationRequest request) {
		return request.getAttribute(PET_KIND);
	}
}
