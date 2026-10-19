package com.tomochi.user.controller;

import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.user.dto.MeResponse;
import com.tomochi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내 정보")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

	private final UserService userService;

	@Operation(summary = "내 정보 조회")
	@GetMapping
	public MeResponse me(@CurrentUser AuthUser user) {
		return userService.findMe(user.userId());
	}
}
