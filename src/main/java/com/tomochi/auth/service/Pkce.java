package com.tomochi.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * PKCE(RFC 7636) S256 방식.
 * 앱이 만든 무작위 {@code code_verifier}의 SHA-256 해시가 {@code code_challenge}예요.
 * 로그인을 시작할 때 challenge만 보내고, 코드를 토큰으로 바꿀 때 verifier를 보내서
 * 처음 로그인을 시작한 앱만 토큰을 받을 수 있게 해요.
 */
public final class Pkce {

	/** S256 challenge = SHA-256(32바이트) → Base64URL(패딩 없음) 43자 */
	private static final Pattern CHALLENGE = Pattern.compile("^[A-Za-z0-9_-]{43}$");
	/** RFC 7636: 43~128자의 unreserved 문자 */
	private static final Pattern VERIFIER = Pattern.compile("^[A-Za-z0-9._~-]{43,128}$");

	private Pkce() {
	}

	public static boolean isValidChallenge(String challenge) {
		return challenge != null && CHALLENGE.matcher(challenge).matches();
	}

	public static boolean matches(String verifier, String challenge) {
		if (verifier == null || challenge == null || !VERIFIER.matcher(verifier).matches()) {
			return false;
		}
		byte[] expected = challenge.getBytes(StandardCharsets.US_ASCII);
		byte[] actual = challengeOf(verifier).getBytes(StandardCharsets.US_ASCII);
		// 비교 시간으로 값을 추측하지 못하게 항상 끝까지 비교해요.
		return MessageDigest.isEqual(expected, actual);
	}

	static String challengeOf(String verifier) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
