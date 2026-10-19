package com.tomochi.app.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AppVersionTest {

	@Test
	void 숫자로_비교한다() {
		assertThat(AppVersion.parse("0.9.0").isOlderThan(AppVersion.parse("0.10.0"))).isTrue();
		assertThat(AppVersion.parse("1.0.0").isOlderThan(AppVersion.parse("0.99.99"))).isFalse();
		assertThat(AppVersion.parse("0.2.1").isOlderThan(AppVersion.parse("0.2.10"))).isTrue();
		assertThat(AppVersion.parse("0.3.0").isOlderThan(AppVersion.parse("0.3.0"))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "1", "1.2", "1.2.3.4", "v1.2.3", "1.2.x", "1.2.3-beta"})
	void 형식이_틀리면_읽지_않는다(String value) {
		assertThat(AppVersion.tryParse(value)).isEmpty();
	}

	@Test
	void 앞뒤_공백은_무시한다() {
		assertThat(AppVersion.tryParse(" 0.3.0 ")).contains(new AppVersion(0, 3, 0));
	}
}
