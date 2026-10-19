package com.tomochi.friend.repository;

import com.tomochi.user.domain.PetKind;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 친구 목록 읽기. 관계·사용자·숨김 설정을 한 쿼리로 읽어요 (친구 수만큼 쿼리를 날리지 않게요).
 */
@Repository
@RequiredArgsConstructor
public class FriendQuery {

	private static final String SELECT = """
			SELECT u.id, u.nickname, u.pet_kind, u.last_seen_at, COALESCE(fs.hidden, FALSE) AS hidden
			FROM friendship f
			JOIN users u ON u.id = IF(f.user_low = ?, f.user_high, f.user_low)
			LEFT JOIN friend_setting fs ON fs.owner_id = ? AND fs.friend_id = u.id
			WHERE (f.user_low = ? OR f.user_high = ?) AND u.deleted_at IS NULL
			""";

	private static final RowMapper<FriendRow> ROW = (rs, rowNum) -> {
		LocalDateTime lastSeen = rs.getObject("last_seen_at", LocalDateTime.class);
		return new FriendRow(
				rs.getLong("id"),
				rs.getString("nickname"),
				PetKind.valueOf(rs.getString("pet_kind")),
				lastSeen != null ? lastSeen.toInstant(ZoneOffset.UTC) : null,
				rs.getBoolean("hidden"));
	};

	private final JdbcTemplate jdbcTemplate;

	public List<FriendRow> findFriends(Long userId) {
		return jdbcTemplate.query(SELECT + " ORDER BY u.id", ROW, userId, userId, userId, userId);
	}

	public Optional<FriendRow> findFriend(Long userId, Long friendId) {
		return jdbcTemplate.query(SELECT + " AND u.id = ?", ROW, userId, userId, userId, userId, friendId)
				.stream().findFirst();
	}

	/**
	 * 내 상태 변화를 받을 친구들. 나를 목록에서 숨긴 친구와 탈퇴한 친구는 빼요.
	 */
	public List<Long> findWatcherIds(Long userId) {
		return jdbcTemplate.queryForList("""
				SELECT u.id
				FROM friendship f
				JOIN users u ON u.id = IF(f.user_low = ?, f.user_high, f.user_low)
				LEFT JOIN friend_setting fs ON fs.owner_id = u.id AND fs.friend_id = ?
				WHERE (f.user_low = ? OR f.user_high = ?) AND u.deleted_at IS NULL AND COALESCE(fs.hidden, FALSE) = FALSE
				""", Long.class, userId, userId, userId, userId);
	}

	/**
	 * @param lastSeenAt 하트비트로 남긴 마지막 접속 시각 (5분 정밀도)
	 * @param hidden     내가 목록에서 숨겼는지
	 */
	public record FriendRow(Long id, String nickname, PetKind petKind, Instant lastSeenAt, boolean hidden) {
	}
}
