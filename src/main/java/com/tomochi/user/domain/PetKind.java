package com.tomochi.user.domain;

import java.util.Arrays;

public enum PetKind {
	CHOCO, BUTTER, HOBBANG, SHERBET, PEANUT, HONEYCOMB;

	public static final PetKind DEFAULT = CHOCO;

	/** 앱이 보낸 값을 읽어요. 비었거나 모르는 값이면 기본 펫(초코)이에요. */
	public static PetKind fromOrDefault(String value) {
		if (value == null) {
			return DEFAULT;
		}
		return Arrays.stream(values())
				.filter(kind -> kind.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElse(DEFAULT);
	}
}
