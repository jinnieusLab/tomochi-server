package com.tomochi.presence.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;

/**
 * 한 사용자의 지금 상태. 기기가 여러 대면 합쳐요: 하나라도 작업 중이면 작업 중, 작업 시작은 가장 이른 기기.
 *
 * @param workingSince 작업 중으로 바뀐 순간. 작업 중이 아니면 null
 */
public record Presence(PresenceStatus status, StatusIcon icon, Instant workingSince) {

	public static final Presence OFFLINE = new Presence(PresenceStatus.OFFLINE, null, null);

	/** 기기 하나의 상태 */
	public record Device(PresenceStatus status, StatusIcon icon, Instant workingSince, Instant updatedAt) {
	}

	public static Presence merge(Collection<Device> devices) {
		if (devices.isEmpty()) {
			return OFFLINE;
		}
		Device best = devices.stream()
				.min(Comparator.comparingInt((Device device) -> rank(device.status()))
						.thenComparing(device -> device.workingSince() != null ? device.workingSince() : Instant.MAX))
				.orElseThrow();
		if (best.status() != PresenceStatus.WORKING) {
			// 쉬는 중·자리 비움에는 아이콘을 보여주지 않아요.
			return new Presence(best.status(), null, null);
		}
		return new Presence(PresenceStatus.WORKING, best.icon(), best.workingSince());
	}

	private static int rank(PresenceStatus status) {
		return switch (status) {
			case WORKING -> 0;
			case RESTING -> 1;
			case AWAY -> 2;
			case OFFLINE -> 3;
		};
	}
}
