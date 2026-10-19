package com.tomochi.auth.controller;

import com.tomochi.auth.dto.SessionResponse;
import com.tomochi.auth.service.SessionService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SessionController {

	private final SessionService sessionService;

	@Operation(summary = "로그아웃",
			description = "지금 기기의 Refresh Token을 모두 폐기해요. 이미 받은 Access Token은 만료(최대 30분)까지 유효해요.")
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@CurrentUser AuthUser user) {
		sessionService.logout(user);
	}

	@Operation(summary = "로그인한 기기 목록", description = "최근에 쓴 기기부터. `current`가 지금 기기예요.")
	@GetMapping("/sessions")
	public List<SessionResponse> sessions(@CurrentUser AuthUser user) {
		return sessionService.findSessions(user);
	}

	@Operation(summary = "다른 기기 로그아웃")
	@DeleteMapping("/sessions/{sessionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(@CurrentUser AuthUser user, @PathVariable String sessionId) {
		sessionService.revoke(user, sessionId);
	}
}
