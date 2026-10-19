package com.tomochi.presence.domain;

/** 앱이 보내는 상태. OFFLINE은 보내지 않아요. 하트비트가 끊기면 서버가 OFFLINE으로 봐요. */
public enum PresenceStatus {
	/** 작업 중 */
	WORKING,
	/** 쉬는 중 (2분 무입력) */
	RESTING,
	/** 자리 비움 (15분 무입력) */
	AWAY,
	/** 앱이 꺼졌거나 하트비트가 90초 넘게 끊김 (서버가 정해요) */
	OFFLINE
}
