package com.tomochi.user.service;

import com.tomochi.user.domain.UserRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 6자리 초대 코드. 헷갈리는 글자(0/O, 1/I/L)를 뺀 31자 중에서 뽑아요 (31^6 ≈ 8억 8천만 가지).
 */
@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

	static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	static final int LENGTH = 6;
	private static final int MAX_ATTEMPTS = 10;

	private final SecureRandom random = new SecureRandom();
	private final UserRepository userRepository;

	/**
	 * 아직 쓰이지 않은 코드를 만들어요. 동시에 같은 코드가 뽑히는 드문 경우는
	 * {@code invite_code} 유니크 제약이 마지막으로 막아요.
	 */
	public String generateUnique() {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String code = generate();
			if (!userRepository.existsByInviteCode(code)) {
				return code;
			}
		}
		throw new IllegalStateException("초대 코드를 만들지 못했어요");
	}

	String generate() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}
}
