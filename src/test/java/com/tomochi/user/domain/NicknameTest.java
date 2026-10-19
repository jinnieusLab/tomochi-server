package com.tomochi.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.error.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NicknameTest {

	@Test
	void 앞뒤_공백을_지운다() {
		assertThat(Nickname.normalize("  민지  ")).isEqualTo("민지");
		assertThat(Nickname.normalize("　민지　")).isEqualTo("민지");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "   ", "　"})
	void 비우면_닉네임_없음(String raw) {
		assertThat(Nickname.normalize(raw)).isNull();
	}

	@Test
	void null이면_닉네임_없음() {
		assertThat(Nickname.normalize(null)).isNull();
	}

	@Test
	void 열두_글자까지_된다() {
		assertThat(Nickname.normalize("가나다라마바사아자차카타")).hasSize(12);
	}

	@Test
	void 열세_글자는_안_된다() {
		assertInvalid("가나다라마바사아자차카타파");
	}

	@Test
	void 이모지는_한_글자로_센다() {
		// 🍫는 UTF-16으로 2칸이지만 한 글자예요.
		String twelveEmoji = "🍫".repeat(12);

		assertThat(Nickname.normalize(twelveEmoji)).isEqualTo(twelveEmoji);
		assertInvalid("🍫".repeat(13));
	}

	@Test
	void 이모지를_잇는_ZWJ는_허용한다() {
		assertThat(Nickname.normalize("민지👩‍💻")).isEqualTo("민지👩‍💻");
	}

	@Test
	void 가운데_공백은_그대로_둔다() {
		assertThat(Nickname.normalize("민 지")).isEqualTo("민 지");
	}

	@ParameterizedTest
	@ValueSource(strings = {"민\n지", "민\r지", "민\t지", "민\u0000지", "민 지", "민 지", "민\u007F지"})
	void 줄바꿈과_제어_문자는_안_된다(String raw) {
		assertInvalid(raw);
	}

	private static void assertInvalid(String raw) {
		assertThatThrownBy(() -> Nickname.normalize(raw))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(UserErrorCode.INVALID_NICKNAME);
	}
}
