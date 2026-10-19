package com.tomochi.together.service;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.repository.ActivitySegmentReader;
import com.tomochi.together.domain.Overlap;
import com.tomochi.together.domain.TogetherDays;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 친구 한 쌍의 둘이 함께한 시간을 계산해 {@code together_daily}에 써요.
 * 같은 기간을 다시 돌려도 결과가 같아요 ({@code INSERT ... ON DUPLICATE KEY UPDATE}로 덮어써요).
 */
@Component
@RequiredArgsConstructor
public class TogetherAggregator {

	private final ActivitySegmentReader segmentReader;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public record Member(Long userId, ZoneId zone) {
	}

	/** {@code [from, to)} 안의 겹침을 계산해요. 각자의 달력에서 하루가 통째로 이 안에 든 날만 저장해요. */
	public int aggregate(Member a, Member b, Instant from, Instant to) {
		List<TimeRange> overlaps = overlaps(a.userId(), b.userId(), from, to);
		int rows = 0;
		rows += save(a, b.userId(), TogetherDays.split(overlaps, a.zone()), from, to);
		rows += save(b, a.userId(), TogetherDays.split(overlaps, b.zone()), from, to);
		return rows;
	}

	/** {@code [from, to)}로 잘라 낸 두 사람의 겹친 구간 */
	public List<TimeRange> overlaps(Long a, Long b, Instant from, Instant to) {
		List<TimeRange> clipped = new ArrayList<>();
		for (TimeRange overlap : Overlap.between(segmentReader.findOverlapping(a, from, to),
				segmentReader.findOverlapping(b, from, to))) {
			TimeRange inWindow = overlap.intersect(from, to);
			if (inWindow != null) {
				clipped.add(inWindow);
			}
		}
		return clipped;
	}

	private int save(Member owner, Long friendId, Map<LocalDate, Long> days, Instant from, Instant to) {
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
		List<Object[]> rows = days.entrySet().stream()
				.filter(day -> isWholeDayInside(day.getKey(), owner.zone(), from, to))
				.map(day -> new Object[] {owner.userId(), friendId, day.getKey(), day.getValue(), now})
				.toList();
		jdbcTemplate.batchUpdate("""
				INSERT INTO together_daily (owner_id, friend_id, date, seconds, updated_at) VALUES (?, ?, ?, ?, ?) AS incoming
				ON DUPLICATE KEY UPDATE seconds = incoming.seconds, updated_at = incoming.updated_at
				""", rows);
		return rows.size();
	}

	/** 기간 가장자리에 걸친 날은 일부만 보여서 저장하지 않아요. 오늘처럼 아직 안 끝난 날도요. */
	private static boolean isWholeDayInside(LocalDate date, ZoneId zone, Instant from, Instant to) {
		Instant dayStart = date.atStartOfDay(zone).toInstant();
		Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
		return !dayStart.isBefore(from) && !dayEnd.isAfter(to);
	}
}
