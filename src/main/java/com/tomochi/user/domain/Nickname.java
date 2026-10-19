package com.tomochi.user.domain;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.error.UserErrorCode;

/**
 * 닉네임 규칙. 친구끼리만 보여서 중복은 허용해요.
 * <ul>
 *   <li>앞뒤 공백을 지우고, 비면 닉네임 없음(null)</li>
 *   <li>1~12자. 이모지 하나도 한 글자로 세요 (코드 포인트 기준, DB {@code VARCHAR(12)}와 같은 기준)</li>
 *   <li>줄바꿈·탭 같은 제어 문자 금지. 이모지를 잇는 ZWJ(U+200D)는 허용해요</li>
 * </ul>
 */
public final class Nickname {

	public static final int MAX_LENGTH = 12;

	private Nickname() {
	}

	/** @return 저장할 닉네임. 비었으면 null */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String nickname = raw.strip();
		if (nickname.isEmpty()) {
			return null;
		}
		if (nickname.codePointCount(0, nickname.length()) > MAX_LENGTH || hasForbiddenCharacter(nickname)) {
			throw new BusinessException(UserErrorCode.INVALID_NICKNAME);
		}
		return nickname;
	}

	private static boolean hasForbiddenCharacter(String nickname) {
		return nickname.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
				|| Character.getType(codePoint) == Character.LINE_SEPARATOR
				|| Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR);
	}
}
