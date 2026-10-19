package com.tomochi.app.controller;

import com.tomochi.app.config.AppVersionProperties;
import com.tomochi.app.dto.AppVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppVersionController implements AppVersionApi {

	private final AppVersionProperties appVersionProperties;

	@Override
	@GetMapping("/version")
	public AppVersionResponse version() {
		return AppVersionResponse.from(appVersionProperties);
	}
}
