package com.tomochi.user.domain;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.error.UserErrorCode;
import java.util.regex.Pattern;

/**
 * 펫에 얹은 장식 id 규칙.
 *
 * <p>장식 목록과 "받았는지"는 지금은 앱이 가지고 있어요 (계절 기간에 토모찌와 1시간 함께하면 받아요).
 * 그래서 서버는 id 모양만 확인하고, 어떤 장식인지·받은 장식인지는 확인하지 않아요.
 * 모르는 id를 받은 앱은 장식 없이 그려요. 장식 목록을 서버가 내려주게 되면 그때 목록에 있는지도 확인해요.
 *
 * <ul>
 *   <li>null 또는 빈 문자열 → 장식 없음(null)</li>
 *   <li>소문자·숫자·하이픈 1~32자 (예: {@code winter-scarf})</li>
 * </ul>
 */
public final class Decoration {

	public static final int MAX_LENGTH = 32;
	private static final Pattern ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

	private Decoration() {
	}

	/** @return 저장할 장식 id. 비었으면 null */
	public static String normalize(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		if (raw.length() > MAX_LENGTH || !ID.matcher(raw).matches()) {
			throw new BusinessException(UserErrorCode.INVALID_DECORATION);
		}
		return raw;
	}
}
