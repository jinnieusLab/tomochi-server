package com.tomochi.presence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresenceTest {

	private static final Instant T0 = Instant.parse("2026-10-17T09:00:00Z");

	@Test
	void 기기가_없으면_오프라인() {
		assertThat(Presence.merge(List.of())).isEqualTo(Presence.OFFLINE);
	}

	@Test
	void 하나라도_작업_중이면_작업_중이고_시작은_가장_이른_기기() {
		Presence merged = Presence.merge(List.of(
				device(PresenceStatus.RESTING, null, null),
				device(PresenceStatus.WORKING, StatusIcon.CODE, T0.plusSeconds(600)),
				device(PresenceStatus.WORKING, StatusIcon.STUDY, T0)));

		assertThat(merged.status()).isEqualTo(PresenceStatus.WORKING);
		assertThat(merged.workingSince()).isEqualTo(T0);
		assertThat(merged.icon()).isEqualTo(StatusIcon.STUDY);
	}

	@Test
	void 작업_중이_아니면_아이콘을_보여주지_않는다() {
		Presence merged = Presence.merge(List.of(
				device(PresenceStatus.AWAY, StatusIcon.CODE, null),
				device(PresenceStatus.RESTING, StatusIcon.STUDY, null)));

		assertThat(merged).isEqualTo(new Presence(PresenceStatus.RESTING, null, null));
	}

	private static Presence.Device device(PresenceStatus status, StatusIcon icon, Instant since) {
		return new Presence.Device(status, icon, since, T0);
	}
}
