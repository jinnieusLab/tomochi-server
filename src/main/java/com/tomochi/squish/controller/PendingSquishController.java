package com.tomochi.squish.controller;

import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.squish.dto.AckSquishesRequest;
import com.tomochi.squish.dto.PendingSquishesResponse;
import com.tomochi.squish.service.PendingSquishService;
import com.tomochi.squish.service.SquishDelivery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/squishes")
@RequiredArgsConstructor
public class PendingSquishController implements PendingSquishApi {

	private final PendingSquishService pendingSquishService;
	private final SquishDelivery squishDelivery;

	@Override
	@GetMapping("/pending")
	public PendingSquishesResponse pending(@CurrentUser AuthUser user) {
		return pendingSquishService.findPending(user.userId());
	}

	@Override
	@PostMapping("/ack")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acknowledge(@CurrentUser AuthUser user, @Valid @RequestBody AckSquishesRequest request) {
		squishDelivery.acknowledge(user.userId(), request.squishIds());
	}
}
