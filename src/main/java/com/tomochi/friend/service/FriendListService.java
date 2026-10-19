package com.tomochi.friend.service;

import com.tomochi.friend.dto.FriendListResponse;
import com.tomochi.friend.dto.FriendSummaryResponse;
import com.tomochi.friend.error.FriendErrorCode;
import com.tomochi.friend.repository.FriendQuery;
import com.tomochi.friend.repository.FriendQuery.FriendRow;
import com.tomochi.global.error.BusinessException;
import com.tomochi.presence.domain.Presence;
import com.tomochi.presence.service.PresenceStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 친구 목록. 처음에는 앱이 팝오버를 열 때와 열려 있는 동안 30초마다 불러요 (WebSocket 전환은 PRES-2).
 * 친구 정보는 MySQL 쿼리 한 번, 상태는 Redis pipeline 한 번으로 읽어요.
 */
@Service
@RequiredArgsConstructor
public class FriendListService {

	private final FriendQuery friendQuery;
	private final PresenceStore presenceStore;
	private final Clock clock;

	public FriendListResponse findFriends(Long userId, boolean includeHidden) {
		List<FriendRow> rows = friendQuery.findFriends(userId);
		int hiddenCount = (int) rows.stream().filter(FriendRow::hidden).count();
		List<FriendRow> shown = includeHidden ? rows : rows.stream().filter(row -> !row.hidden()).toList();

		Map<Long, Presence> presences = presenceStore.findAll(shown.stream().map(FriendRow::id).toList(), clock.instant());
		List<FriendSummaryResponse> friends = shown.stream()
				.map(row -> FriendSummaryResponse.of(row, presences.get(row.id())))
				.toList();
		return new FriendListResponse(friends, hiddenCount);
	}

	public FriendSummaryResponse findFriend(Long userId, Long friendId) {
		FriendRow row = friendQuery.findFriend(userId, friendId)
				.orElseThrow(() -> new BusinessException(FriendErrorCode.FRIEND_NOT_FOUND));
		Instant now = clock.instant();
		return FriendSummaryResponse.of(row, presenceStore.find(friendId, now));
	}
}
