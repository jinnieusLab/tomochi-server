package com.tomochi.user.domain;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.error.UserErrorCode;

/**
 * 닉네임 규칙. 친구끼리만 보여서 중복은 허용해요.
 * <ul>
 *   <li>앞뒤 공백(NBSP 같은 유니코드 공백 포함)을 지우고, 비면 닉네임 없음(null)</li>
 *   <li>1~12자. 이모지 하나도 한 글자로 세요 (코드 포인트 기준, DB {@code VARCHAR(12)}와 같은 기준)</li>
 *   <li>줄바꿈·탭 같은 제어 문자 금지. 앞뒤에 있어도 지우지 않고 거절해요. 이모지를 잇는 ZWJ(U+200D)는 허용해요</li>
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
		// 공백을 지우기 전에 검사해요. 먼저 지우면 "\n민지"의 줄바꿈이 사라져 통과해 버려요.
		if (hasForbiddenCharacter(raw)) {
			throw new BusinessException(UserErrorCode.INVALID_NICKNAME);
		}
		String nickname = stripSpaces(raw);
		if (nickname.isEmpty()) {
			return null;
		}
		if (nickname.codePointCount(0, nickname.length()) > MAX_LENGTH) {
			throw new BusinessException(UserErrorCode.INVALID_NICKNAME);
		}
		return nickname;
	}

	/** {@link String#strip()}은 NBSP(U+00A0)를 공백으로 보지 않아서, 유니코드 공백 문자까지 지워요. */
	private static String stripSpaces(String value) {
		int start = 0;
		int end = value.length();
		while (start < end && isSpace(value.codePointAt(start))) {
			start += Character.charCount(value.codePointAt(start));
		}
		while (end > start && isSpace(value.codePointBefore(end))) {
			end -= Character.charCount(value.codePointBefore(end));
		}
		return value.substring(start, end);
	}

	private static boolean isSpace(int codePoint) {
		return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
	}

	private static boolean hasForbiddenCharacter(String nickname) {
		return nickname.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
				|| Character.getType(codePoint) == Character.LINE_SEPARATOR
				|| Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR);
	}
}
