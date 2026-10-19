package com.tomochi.together.service;

import com.tomochi.activity.domain.TimeRange;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.friend.repository.FriendQuery;
import com.tomochi.global.error.BusinessException;
import com.tomochi.together.domain.TogetherDays;
import com.tomochi.together.dto.TogetherResponse;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import com.tomochi.user.error.UserErrorCode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 친구별 함께한 날 잔디. 지난 날은 매일 배치가 저장한 {@code together_daily}를 읽기만 해요 (17주 × 7줄).
 * 오늘과 어제는 배치가 아직 안 돌았을 수 있어서 즉석으로 계산하고, 같은 친구 쌍은 5분 동안 Redis에 둬요.
 */
@Service
@RequiredArgsConstructor
public class TogetherQueryService {

	public static final int MAX_WEEKS = 17;
	static final Duration LIVE_CACHE = Duration.ofMinutes(5);
	private static final int LIVE_DAYS = 2;

	private final UserRepository userRepository;
	private final FriendQuery friendQuery;
	private final TogetherAggregator aggregator;
	private final JdbcTemplate jdbcTemplate;
	private final StringRedisTemplate redisTemplate;
	private final Clock clock;

	public TogetherResponse findTogether(Long ownerId, Long friendId, int weeks) {
		if (friendQuery.findFriend(ownerId, friendId).isEmpty()) {
			throw new BusinessException(FriendErrorCode.FRIEND_NOT_FOUND);
		}
		ZoneId zone = zoneOf(ownerId);
		LocalDate today = LocalDate.ofInstant(clock.instant(), zone);
		LocalDate from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).minusWeeks(weeks - 1L);

		Map<LocalDate, Long> seconds = new TreeMap<>(stored(ownerId, friendId, from, today.minusDays(LIVE_DAYS)));
		for (LocalDate day = today.minusDays(LIVE_DAYS - 1L); !day.isAfter(today); day = day.plusDays(1)) {
			if (!day.isBefore(from)) {
				long live = liveSeconds(ownerId, friendId, day, zone);
				if (live > 0) {
					seconds.put(day, live);
				}
			}
		}
		List<TogetherResponse.Day> days = seconds.entrySet().stream()
				.map(day -> new TogetherResponse.Day(day.getKey(), ceilMinutes(day.getValue())))
				.toList();
		return new TogetherResponse(from, today, days, days.size());
	}

	/**
	 * {@code [from, to]}(내 시간대) 동안 친구 누구와든 1초라도 겹친 날 수. 메인의 "이번 주 친구와 N일"이에요.
	 * 지난 날은 저장된 집계에서 날짜만 세고, 오늘·어제는 친구마다 즉석 계산(5분 캐시)해요. 친구는 20명까지라 가벼워요.
	 */
	public int countTogetherDays(Long ownerId, LocalDate from, LocalDate to) {
		ZoneId zone = zoneOf(ownerId);
		LocalDate liveFrom = to.minusDays(LIVE_DAYS - 1L);
		Set<LocalDate> days = new HashSet<>(jdbcTemplate.queryForList(
				"SELECT DISTINCT date FROM together_daily WHERE owner_id = ? AND date BETWEEN ? AND ? AND seconds > 0",
				LocalDate.class, ownerId, from, liveFrom.minusDays(1)));
		List<Long> friendIds = friendQuery.findFriends(ownerId).stream().map(FriendQuery.FriendRow::id).toList();
		for (LocalDate day = liveFrom.isBefore(from) ? from : liveFrom; !day.isAfter(to); day = day.plusDays(1)) {
			for (Long friendId : friendIds) {
				if (liveSeconds(ownerId, friendId, day, zone) > 0) {
					days.add(day);
					break;
				}
			}
		}
		return days.size();
	}

	/** 내 시간대로 그날 친구와 겹친 초. 5분 캐시 */
	public long liveSeconds(Long ownerId, Long friendId, LocalDate day, ZoneId zone) {
		String key = "together:today:" + ownerId + ":" + friendId + ":" + day;
		String cached = redisTemplate.opsForValue().get(key);
		if (cached != null) {
			return Long.parseLong(cached);
		}
		Instant dayStart = day.atStartOfDay(zone).toInstant();
		Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
		List<TimeRange> overlaps = aggregator.overlaps(ownerId, friendId, dayStart, dayEnd);
		long value = TogetherDays.split(overlaps, zone).getOrDefault(day, 0L);
		redisTemplate.opsForValue().set(key, String.valueOf(value), LIVE_CACHE);
		return value;
	}

	private Map<LocalDate, Long> stored(Long ownerId, Long friendId, LocalDate from, LocalDate to) {
		Map<LocalDate, Long> seconds = new TreeMap<>();
		jdbcTemplate.query("""
						SELECT date, seconds FROM together_daily
						WHERE owner_id = ? AND friend_id = ? AND date BETWEEN ? AND ? AND seconds > 0
						""",
				rs -> {
					seconds.put(rs.getObject(1, LocalDate.class), rs.getLong(2));
				},
				ownerId, friendId, from, to);
		return seconds;
	}

	ZoneId zoneOf(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		return ZoneId.of(user.getTimezone());
	}

	private static long ceilMinutes(long seconds) {
		return (seconds + 59) / 60;
	}
}
