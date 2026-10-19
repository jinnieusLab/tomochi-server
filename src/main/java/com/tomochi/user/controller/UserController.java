package com.tomochi.user.controller;

import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.user.dto.UpdateUserRequest;
import com.tomochi.user.dto.UserResponse;
import com.tomochi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController implements UserApi {

	private final UserService userService;

	@Override
	@GetMapping
	public UserResponse find(@CurrentUser AuthUser user) {
		return userService.findUser(user.userId());
	}

	@Override
	@PatchMapping
	public UserResponse update(@CurrentUser AuthUser user, @RequestBody UpdateUserRequest request) {
		return userService.updateUser(user.userId(), request);
	}
}
