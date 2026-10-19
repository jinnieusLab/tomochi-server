package com.tomochi.friend.controller;

import com.tomochi.friend.dto.AddFriendRequest;
import com.tomochi.friend.dto.FriendResponse;
import com.tomochi.friend.service.FriendshipFacade;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController implements FriendApi {

	private final FriendshipFacade friendshipFacade;

	@Override
	@PostMapping
	public FriendResponse add(@CurrentUser AuthUser user, @Valid @RequestBody AddFriendRequest request) {
		return friendshipFacade.addByInviteCode(user.userId(), request.inviteCode());
	}
}
