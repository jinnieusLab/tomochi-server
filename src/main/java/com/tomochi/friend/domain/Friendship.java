package com.tomochi.friend.domain;

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
import org.hibernate.annotations.CreationTimestamp;

/**
 * 두 사람의 친구 관계. 방향이 없어서 늘 작은 id를 {@code userLow}에 둬요.
 */
@Entity
@Table(name = "friendship")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userLow;

	@Column(nullable = false)
	private Long userHigh;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Friendship(Long userLow, Long userHigh) {
		this.userLow = userLow;
		this.userHigh = userHigh;
	}

	public static Friendship between(Long a, Long b) {
		if (a.equals(b)) {
			throw new IllegalArgumentException("자기 자신과는 친구가 될 수 없어요");
		}
		return new Friendship(Math.min(a, b), Math.max(a, b));
	}

	public Long otherThan(Long userId) {
		return userLow.equals(userId) ? userHigh : userLow;
	}
}
