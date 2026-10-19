package com.tomochi.app.config;

import com.tomochi.app.domain.AppVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 맥 앱 버전 안내. 공증 없는 DMG라 자동 업데이트가 없어서, 서버가 "새 버전을 받아 주세요"를 알려요.
 * 새 버전을 낼 때 환경 변수(APP_LATEST_VERSION, APP_MINIMUM_VERSION)만 바꾸면 돼요.
 *
 * @param latest      지금 받을 수 있는 최신 버전. 이보다 낮으면 앱이 조용히 업데이트를 권해요
 * @param minimum     이보다 낮은 앱은 API가 맞지 않아서 426으로 막아요
 * @param downloadUrl 새 버전을 받는 곳
 */
@ConfigurationProperties("tomochi.app")
public record AppVersionProperties(
		@DefaultValue("0.1.0") String latest,
		@DefaultValue("0.1.0") String minimum,
		@DefaultValue("https://github.com/jinnieusLab/tomochi/releases/latest") String downloadUrl
) {

	public AppVersionProperties {
		if (AppVersion.parse(minimum).compareTo(AppVersion.parse(latest)) > 0) {
			throw new IllegalArgumentException("최소 버전(" + minimum + ")이 최신 버전(" + latest + ")보다 높아요");
		}
	}

	public AppVersion minimumVersion() {
		return AppVersion.parse(minimum);
	}
}
