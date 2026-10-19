package com.tomochi.app.dto;

import com.tomochi.app.config.AppVersionProperties;

public record AppVersionResponse(
		String latest,
		String minimum,
		String downloadUrl
) {

	public static AppVersionResponse from(AppVersionProperties properties) {
		return new AppVersionResponse(properties.latest(), properties.minimum(), properties.downloadUrl());
	}
}
