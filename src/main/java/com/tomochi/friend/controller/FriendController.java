package com.tomochi.friend.controller;

import com.tomochi.friend.dto.AddFriendRequest;
import com.tomochi.friend.dto.FriendListResponse;
import com.tomochi.friend.dto.FriendResponse;
import com.tomochi.friend.dto.FriendSummaryResponse;
import com.tomochi.friend.service.FriendListService;
import com.tomochi.friend.service.FriendSettingService;
import com.tomochi.friend.service.UnfriendService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.tomochi.friend.service.FriendshipFacade;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final FriendSettingService friendSettingService;
	private final UnfriendService unfriendService;

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

	@Override
	@GetMapping("/{friendId}")
	public FriendSummaryResponse find(@CurrentUser AuthUser user, @PathVariable Long friendId) {
		return friendListService.findFriend(user.userId(), friendId);
	}

	@Override
	@PutMapping("/{friendId}/hidden")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void hide(@CurrentUser AuthUser user, @PathVariable Long friendId) {
		friendSettingService.setHidden(user.userId(), friendId, true);
	}

	@Override
	@DeleteMapping("/{friendId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfriend(@CurrentUser AuthUser user, @PathVariable Long friendId) {
		unfriendService.unfriend(user.userId(), friendId);
	}

	@Override
	@DeleteMapping("/{friendId}/hidden")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unhide(@CurrentUser AuthUser user, @PathVariable Long friendId) {
		friendSettingService.setHidden(user.userId(), friendId, false);
	}
}
