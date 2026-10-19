package com.tomochi.squish.controller;

import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.squish.service.SquishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SquishController implements SquishApi {

	private final SquishService squishService;

	@Override
	@PostMapping("/api/v1/friends/{friendId}/squishes")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void send(@CurrentUser AuthUser user, @PathVariable Long friendId) {
		squishService.send(user.userId(), friendId);
	}
}
