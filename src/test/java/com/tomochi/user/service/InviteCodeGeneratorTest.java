package com.tomochi.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tomochi.user.domain.UserRepository;
import org.junit.jupiter.api.Test;

class InviteCodeGeneratorTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final InviteCodeGenerator generator = new InviteCodeGenerator(userRepository);

	@Test
	void 헷갈리는_글자를_뺀_6자리_코드를_만든다() {
		for (int i = 0; i < 1_000; i++) {
			String code = generator.generate();

			assertThat(code).hasSize(6).matches("[A-Z2-9]{6}").doesNotContainAnyWhitespaces();
			assertThat(code).doesNotContain("0", "O", "1", "I", "L");
		}
	}

	@Test
	void 알파벳은_31자다() {
		assertThat(InviteCodeGenerator.ALPHABET).hasSize(31);
		assertThat(InviteCodeGenerator.ALPHABET.chars().distinct().count()).isEqualTo(31);
	}

	@Test
	void 이미_쓰이는_코드면_다시_뽑는다() {
		given(userRepository.existsByInviteCode(anyString())).willReturn(true, true, false);

		assertThat(generator.generateUnique()).hasSize(6);
	}

	@Test
	void 계속_겹치면_포기한다() {
		given(userRepository.existsByInviteCode(anyString())).willReturn(true);

		assertThatThrownBy(generator::generateUnique).isInstanceOf(IllegalStateException.class);
	}
}
