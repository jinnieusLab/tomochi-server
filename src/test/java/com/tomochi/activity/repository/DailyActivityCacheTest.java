package com.tomochi.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DailyActivityCacheTest {

	@Test
	void 칸_48개를_비트로_바꿨다가_되돌린다() {
		String slots = "100000000000000000111110001110110000000011100001";

		assertThat(DailyActivityCache.toSlots(DailyActivityCache.toBits(slots))).isEqualTo(slots);
		assertThat(DailyActivityCache.toBits("1" + "0".repeat(47))).isEqualTo(1L);
		assertThat(DailyActivityCache.toBits("0".repeat(47) + "1")).isEqualTo(1L << 47);
		assertThat(DailyActivityCache.toSlots(0L)).isEqualTo("0".repeat(48));
	}
}
