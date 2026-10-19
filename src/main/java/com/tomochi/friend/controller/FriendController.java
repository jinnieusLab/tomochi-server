package com.tomochi.friend.controller;

import com.tomochi.friend.dto.AddFriendRequest;
import com.tomochi.friend.dto.FriendListResponse;
import com.tomochi.friend.dto.FriendResponse;
import com.tomochi.friend.service.FriendListService;
import com.tomochi.friend.service.FriendshipFacade;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController implements FriendApi {

	private final FriendshipFacade friendshipFacade;
	private final FriendListService friendListService;

	@Override
	@PostMapping
	public FriendResponse add(@CurrentUser AuthUser user, @Valid @RequestBody AddFriendRequest request) {
		return friendshipFacade.addByInviteCode(user.userId(), request.inviteCode());
	}

	@Override
	@GetMapping
	public FriendListResponse list(@CurrentUser AuthUser user,
			@RequestParam(defaultValue = "false") boolean includeHidden) {
		return friendListService.findFriends(user.userId(), includeHidden);
	}
}
