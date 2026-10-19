package com.tomochi.app.dto;

import com.tomochi.app.config.AppVersionProperties;
import io.swagger.v3.oas.annotations.media.Schema;

public record AppVersionResponse(
		@Schema(description = "지금 받을 수 있는 최신 버전. 이보다 낮으면 조용히 업데이트를 권해요", example = "0.3.0")
		String latest,

		@Schema(description = "이보다 낮은 앱은 API가 맞지 않아 426을 받아요. 꼭 업데이트해야 해요", example = "0.2.0")
		String minimum,

		@Schema(description = "새 버전을 받는 곳", example = "https://github.com/jinnieusLab/tomochi/releases/latest")
		String downloadUrl
) {

	public static AppVersionResponse from(AppVersionProperties properties) {
		return new AppVersionResponse(properties.latest(), properties.minimum(), properties.downloadUrl());
	}
}
