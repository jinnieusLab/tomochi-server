package com.tomochi.global.security;

/**
 * 지금 요청을 보낸 사용자.
 *
 * @param userId    사용자 id (JWT sub)
 * @param sessionId 로그인한 기기의 세션 id (JWT sid, Refresh Token family id)
 */
public record AuthUser(Long userId, String sessionId) {
}
