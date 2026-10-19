package com.tomochi.together.domain;

import com.tomochi.activity.domain.SegmentUnion;
import com.tomochi.activity.domain.TimeRange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 두 사람의 작업 구간이 겹친 부분. "둘이 함께한 시간"이에요.
 *
 * <p>각자의 구간을 먼저 합집합으로 합쳐요(기기 여러 대를 켜 둬도 두 번 세지 않게). 그러면 두 목록은 각각 겹침 없이
 * 시작 시각 순이라, 두 포인터로 한 번 훑으면 돼요: O(n + m).
 * <ol>
 *   <li>{@code [max(a.start, b.start), min(a.end, b.end))}가 비어 있지 않으면 겹침에 더해요</li>
 *   <li>끝이 먼저 오는 쪽 포인터를 넘겨요 (그 구간은 더 겹칠 상대가 없어요)</li>
 * </ol>
 */
public final class Overlap {

	private Overlap() {
	}

	public static List<TimeRange> between(Collection<TimeRange> aSegments, Collection<TimeRange> bSegments) {
		List<TimeRange> a = SegmentUnion.of(aSegments);
		List<TimeRange> b = SegmentUnion.of(bSegments);
		List<TimeRange> overlaps = new ArrayList<>();
		int i = 0;
		int j = 0;
		while (i < a.size() && j < b.size()) {
			TimeRange x = a.get(i);
			TimeRange y = b.get(j);
			Instant start = x.start().isAfter(y.start()) ? x.start() : y.start();
			Instant end = x.end().isBefore(y.end()) ? x.end() : y.end();
			if (end.isAfter(start)) {
				overlaps.add(new TimeRange(start, end));
			}
			if (x.end().isBefore(y.end())) {
				i++;
			} else {
				j++;
			}
		}
		return overlaps;
	}
}
