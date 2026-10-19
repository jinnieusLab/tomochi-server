package com.tomochi.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.tomochi.presence.domain.StatusIcon;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * {@link DynamicUpdate}: 바뀐 컬럼만 UPDATE해요. 기본값(모든 컬럼 UPDATE)이면 닉네임 변경과 펫 변경이 동시에 올 때
 * 나중에 커밋한 쪽이 읽어 둔 옛 값으로 다른 필드를 덮어써서 먼저 바꾼 값이 사라져요 (lost update).
 */
@Entity
@DynamicUpdate
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	/** 시간대를 아직 받지 못했을 때 쓰는 값. 앱이 켜질 때 맥의 시간대로 바꿔요 (USER-5). */
	public static final String DEFAULT_TIMEZONE = "Asia/Seoul";
	/** 탈퇴 후 이 기간 안에 다시 로그인하면 계정을 되살려요. 지나면 정리 배치가 모두 지워요. */
	public static final Duration WITHDRAWAL_GRACE = Duration.ofDays(7);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 12)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PetKind petKind;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private StatusIcon statusIcon;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(nullable = false, unique = true, columnDefinition = "char(6)")
	private String inviteCode;

	private Instant lastSeenAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	private Instant deletedAt;

	private User(PetKind petKind, String inviteCode) {
		this.petKind = petKind;
		this.inviteCode = inviteCode;
		this.timezone = DEFAULT_TIMEZONE;
	}

	/** 처음 로그인한 사용자. 닉네임은 비어 있고, 펫은 앱에서 고른 값이에요. */
	public static User signUp(PetKind petKind, String inviteCode) {
		return new User(petKind, inviteCode);
	}

	/** @param timezone {@link Timezone#validate}를 거친 IANA 시간대 이름 */
	public void withdraw(Instant now) {
		if (deletedAt == null) {
			deletedAt = now;
		}
	}

	public void restore() {
		deletedAt = null;
	}

	public boolean isWithdrawn() {
		return deletedAt != null;
	}

	/** 탈퇴했고 유예 기간도 지나서, 남은 데이터를 지워야 하는 계정 */
	public boolean isPastWithdrawalGrace(Instant now) {
		return deletedAt != null && !deletedAt.plus(WITHDRAWAL_GRACE).isAfter(now);
	}

	/** @param statusIcon null이면 아이콘 없음 */
	public void changeStatusIcon(StatusIcon statusIcon) {
		this.statusIcon = statusIcon;
	}

	/** 이전 코드는 이 순간부터 쓸 수 없어요. */
	public void changeInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}

	public void changeTimezone(String timezone) {
		this.timezone = timezone;
	}

	public void changePetKind(PetKind petKind) {
		this.petKind = petKind;
	}

	/** @param nickname {@link Nickname#normalize}를 거친 값. null이면 닉네임 없음 */
	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}
}
