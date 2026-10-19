package com.tomochi.friend.controller;

import com.tomochi.friend.dto.InviteCodeResponse;
import com.tomochi.friend.service.InviteCodeService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InviteCodeController implements InviteCodeApi {

	private final InviteCodeService inviteCodeService;

	@Override
	@PostMapping("/api/v1/me/invite-code/regenerate")
	public InviteCodeResponse regenerate(@CurrentUser AuthUser user) {
		return inviteCodeService.regenerate(user.userId());
	}
}
