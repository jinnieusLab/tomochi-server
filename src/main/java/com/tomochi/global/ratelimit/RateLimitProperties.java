package com.tomochi.global.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 요청 제한. 1분 고정 창으로 세요.
 *
 * @param authPerIp       로그인 시작·토큰 교환·재발급, IP당
 * @param uploadPerUser   작업 구간 업로드, 사용자당
 * @param defaultPerUser  그 밖의 API, 사용자당 (로그인 안 한 요청은 IP당)
 * @param friendAddPerMinute 초대 코드로 친구 맺기, 사용자당 1분에 (코드 맞히기 방지)
 * @param friendAddPerDay    초대 코드로 친구 맺기, 사용자당 하루에
 * @param squishPerUser      꾹 보내기, 사용자당 1분에 (같은 친구 10초 대기와 따로)
 */
@ConfigurationProperties("tomochi.rate-limit")
public record RateLimitProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("1m") Duration window,
		@DefaultValue("20") int authPerIp,
		@DefaultValue("6") int uploadPerUser,
		@DefaultValue("120") int defaultPerUser,
		@DefaultValue("10") int friendAddPerMinute,
		@DefaultValue("50") int friendAddPerDay,
		@DefaultValue("30") int squishPerUser
) {
}
