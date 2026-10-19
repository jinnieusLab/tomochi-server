package com.tomochi.auth.dto;

import java.time.Instant;

/**
 * 로그인한 기기 하나.
 *
 * @param id         세션 id. 다른 기기를 로그아웃할 때 써요
 * @param signedInAt 이 기기에서 로그인한 시각
 * @param lastUsedAt 마지막으로 토큰을 재발급한 시각 (앱이 켜져 있으면 30분마다 바뀌어요)
 * @param current    지금 요청을 보낸 기기인지
 */
public record SessionResponse(
		String id,
		String deviceName,
		Instant signedInAt,
		Instant lastUsedAt,
		boolean current
) {
}
