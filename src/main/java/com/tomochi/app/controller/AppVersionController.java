package com.tomochi.app.controller;

import com.tomochi.app.config.AppVersionProperties;
import com.tomochi.app.dto.AppVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "앱")
@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppVersionController {

	private final AppVersionProperties appVersionProperties;

	@Operation(summary = "앱 버전 확인", description = """
			로그인 없이 불러요. 앱 버전이 `minimum`보다 낮으면 업데이트를 꼭 받아야 하고,
			`latest`보다만 낮으면 조용히 권하면 돼요.""")
	@SecurityRequirements
	@GetMapping("/version")
	public AppVersionResponse version() {
		return AppVersionResponse.from(appVersionProperties);
	}
}
