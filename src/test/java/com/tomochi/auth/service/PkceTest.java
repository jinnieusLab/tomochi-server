package com.tomochi.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PkceTest {

	// Python hashlib으로 따로 계산한 값: base64url(sha256(verifier)), 패딩 없음
	private static final String VERIFIER = "dBjftJeZ4CVP-mA92Q5qwOFxy4n2PDwq9jJlyfpoPDw";
	private static final String CHALLENGE = "uVzdJH4DNvZMUVLwHRcytsh0mwMr9gBB_R7DxseoPrw";

	@Test
	void verifier의_SHA_256이_challenge와_같으면_맞다() {
		assertThat(Pkce.challengeOf(VERIFIER)).isEqualTo(CHALLENGE);
		assertThat(Pkce.matches(VERIFIER, CHALLENGE)).isTrue();
	}

	@Test
	void 다른_verifier는_틀리다() {
		assertThat(Pkce.matches(VERIFIER.replace('d', 'e'), CHALLENGE)).isFalse();
	}

	@Test
	void 형식에_맞지_않는_verifier는_틀리다() {
		assertThat(Pkce.matches("too-short", CHALLENGE)).isFalse();
		assertThat(Pkce.matches(null, CHALLENGE)).isFalse();
		assertThat(Pkce.matches(VERIFIER + "!", CHALLENGE)).isFalse();
	}

	@Test
	void challenge는_Base64URL_43자여야_한다() {
		assertThat(Pkce.isValidChallenge(CHALLENGE)).isTrue();
		assertThat(Pkce.isValidChallenge(CHALLENGE + "=")).isFalse();
		assertThat(Pkce.isValidChallenge(CHALLENGE.substring(1))).isFalse();
		assertThat(Pkce.isValidChallenge("plain-text-challenge")).isFalse();
		assertThat(Pkce.isValidChallenge(null)).isFalse();
	}
}
