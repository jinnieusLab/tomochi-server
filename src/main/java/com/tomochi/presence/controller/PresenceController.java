package com.tomochi.presence.controller;

import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import com.tomochi.presence.dto.PresenceRequest;
import com.tomochi.presence.dto.StatusIconRequest;
import com.tomochi.presence.service.StatusIconService;
import com.tomochi.presence.service.PresenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PresenceController implements PresenceApi {

	private final PresenceService presenceService;
	private final StatusIconService statusIconService;

	@Override
	@PutMapping("/api/v1/presence")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void heartbeat(@CurrentUser AuthUser user, @Valid @RequestBody PresenceRequest request) {
		presenceService.heartbeat(user, request);
	}

	@Override
	@PutMapping("/api/v1/me/status-icon")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changeStatusIcon(@CurrentUser AuthUser user, @RequestBody StatusIconRequest request) {
		statusIconService.change(user.userId(), request.icon());
	}
}
