package com.tomochi.activity.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 여러 구간의 합집합. 기기 여러 대의 구간이 겹쳐도 시간을 두 번 세지 않게 해요.
 * 시작 시각 순으로 정렬한 뒤 앞에서부터 이어 붙여요: O(n log n).
 *
 * <pre>
 * 맥북    |■■■■■■■■|
 * 아이맥       |■■■■■■■■■■■■|      |■■|
 * 합집합  |■■■■■■■■■■■■■■■■■|      |■■|
 * </pre>
 */
public final class SegmentUnion {

	private SegmentUnion() {
	}

	/** @return 서로 겹치지 않고 시작 시각 순으로 정렬된 구간들 */
	public static List<TimeRange> of(Collection<TimeRange> ranges) {
		List<TimeRange> sorted = ranges.stream()
				.sorted(Comparator.comparing(TimeRange::start))
				.toList();
		List<TimeRange> merged = new ArrayList<>();
		for (TimeRange range : sorted) {
			if (!merged.isEmpty() && !range.start().isAfter(merged.getLast().end())) {
				TimeRange last = merged.removeLast();
				merged.add(new TimeRange(last.start(), range.end().isAfter(last.end()) ? range.end() : last.end()));
			} else {
				merged.add(range);
			}
		}
		return merged;
	}
}
