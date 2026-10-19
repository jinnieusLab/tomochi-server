package com.tomochi.user.domain;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.error.UserErrorCode;
import java.util.Arrays;

public enum PetKind {
	CHOCO, BUTTER, HOBBANG, SHERBET, PEANUT, HONEYCOMB;

	public static final PetKind DEFAULT = CHOCO;

	/** 펫 바꾸기. 정해진 6종이 아니거나 비었으면 거절해요. */
	public static PetKind from(String value) {
		return Arrays.stream(values())
				.filter(kind -> kind.name().equals(value))
				.findFirst()
				.orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_PET_KIND));
	}

	/** 첫 로그인 때 앱이 보낸 값을 읽어요. 비었거나 모르는 값이면 기본 펫(초코)이에요. */
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
