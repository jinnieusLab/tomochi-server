package com.tomochi.auth.domain;

import java.util.Arrays;
import java.util.Optional;

public enum OAuthProvider {
	GOOGLE, GITHUB;

	/** Spring Security의 registrationId(google, github)로 찾아요. */
	public static Optional<OAuthProvider> fromRegistrationId(String registrationId) {
		return Arrays.stream(values())
				.filter(provider -> provider.name().equalsIgnoreCase(registrationId))
				.findFirst();
	}
}
