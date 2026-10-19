package com.tomochi.presence.service;

import com.tomochi.friend.repository.FriendQuery;
import com.tomochi.presence.domain.Presence;
import com.tomochi.realtime.UserMessageSender;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 친구에게 내 상태 변화를 알려요 ({@code presence.changed}). 폴링(30초) 대신 바뀌는 순간 보내서,
 * 친구 목록이 바로 갱신되고 팝오버가 닫혀 있어도 요청이 오가지 않아요. 폴링은 연결이 끊겼을 때 대비로만 남겨요.
 */
@Component
@RequiredArgsConstructor
public class PresenceNotifier {

	private final FriendQuery friendQuery;
	private final UserMessageSender sender;

	/** 친구에게 보이는 값(상태·아이콘·작업 시작)이 바뀌었을 때만 보내요. 30초마다의 같은 하트비트로는 보내지 않아요. */
	public void notifyIfChanged(Long userId, Presence before, Presence after) {
		if (before.equals(after)) {
			return;
		}
		Map<String, Object> data = new HashMap<>();
		data.put("friendId", userId);
		data.put("status", after.status());
		data.put("icon", after.icon());
		data.put("workingSince", after.workingSince() != null ? after.workingSince().toString() : null);
		for (Long watcher : friendQuery.findWatcherIds(userId)) {
			sender.send(watcher, "presence.changed", data);
		}
	}
}
