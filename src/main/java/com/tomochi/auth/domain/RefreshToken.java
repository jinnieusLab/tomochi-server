package com.tomochi.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 한 장. 원문은 앱에만 있고 서버에는 SHA-256 해시만 남겨요.
 * DB가 새어도 해시로는 토큰을 되살릴 수 없어요.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	/** 한 번의 로그인에서 회전으로 이어지는 토큰 묶음. 기기 하나 = family 하나. */
	@Column(nullable = false, columnDefinition = "char(36)")
	private String familyId;

	@Column(nullable = false, unique = true, columnDefinition = "char(64)")
	private String tokenHash;

	@Column(nullable = false, length = 100)
	private String deviceName;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant lastUsedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	public RefreshToken(Long userId, String familyId, String tokenHash, String deviceName,
			Instant now, Instant expiresAt) {
		this.userId = userId;
		this.familyId = familyId;
		this.tokenHash = tokenHash;
		this.deviceName = deviceName;
		this.createdAt = now;
		this.lastUsedAt = now;
		this.expiresAt = expiresAt;
	}
}
