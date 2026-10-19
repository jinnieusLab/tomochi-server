package com.tomochi.together.controller;

import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.together.dto.TogetherResponse;
import com.tomochi.together.service.TogetherQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TogetherController implements TogetherApi {

	private final TogetherQueryService togetherQueryService;

	@Override
	@GetMapping("/api/v1/friends/{friendId}/together")
	public TogetherResponse together(@CurrentUser AuthUser user, @PathVariable Long friendId,
			@RequestParam(defaultValue = "17") int weeks) {
		return togetherQueryService.findTogether(user.userId(), friendId, weeks);
	}
}
