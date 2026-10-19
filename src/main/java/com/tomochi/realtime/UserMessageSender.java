package com.tomochi.realtime;

/**
 * 사용자에게 실시간 메시지 보내기. 지금은 이 서버에 연결된 세션에만 보내요({@link LocalUserMessageSender}).
 * 서버가 여러 대가 되면 Redis Pub/Sub으로 "이 사용자에게 보내기"를 다른 서버에 넘기는 구현으로 바꾸면 돼요.
 * 보내는 쪽 코드(꾹, 상태 변경)는 그대로예요.
 */
public interface UserMessageSender {

	/**
	 * @return 하나 이상의 세션에 보냈으면 true (연결이 없으면 false, 꾹은 나중에 모아서 전달돼요)
	 */
	boolean send(Long userId, String type, Object data);
}
