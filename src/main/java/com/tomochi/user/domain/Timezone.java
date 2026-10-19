package com.tomochi.user.domain;

import com.tomochi.global.error.BusinessException;
import com.tomochi.user.error.UserErrorCode;
import java.time.ZoneId;
import java.util.Set;

/**
 * 사용자 시간대. 시각은 모두 UTC로 저장하고, "오늘"·"이번 주"를 나눌 때만 이 시간대를 써요.
 * {@code +09:00} 같은 고정 오프셋은 받지 않아요. 서머타임이 있는 지역은 오프셋이 바뀌어서 이름으로 받아야 날짜가 맞아요.
 */
public final class Timezone {

	private static final Set<String> AVAILABLE = ZoneId.getAvailableZoneIds();

	private Timezone() {
	}

	public static String validate(String timezone) {
		if (timezone == null || !AVAILABLE.contains(timezone)) {
			throw new BusinessException(UserErrorCode.INVALID_TIMEZONE);
		}
		return timezone;
	}
}
