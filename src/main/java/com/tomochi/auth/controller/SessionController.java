package com.tomochi.auth.controller;

import com.tomochi.auth.dto.SessionResponse;
import com.tomochi.auth.service.SessionService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SessionController implements SessionApi {

	private final SessionService sessionService;

	@Override
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@CurrentUser AuthUser user) {
		sessionService.logout(user);
	}

	@Override
	@GetMapping("/sessions")
	public List<SessionResponse> sessions(@CurrentUser AuthUser user) {
		return sessionService.findSessions(user);
	}

	@Override
	@DeleteMapping("/sessions/{sessionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(@CurrentUser AuthUser user, @PathVariable String sessionId) {
		sessionService.revoke(user, sessionId);
	}
}
