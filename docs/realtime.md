# 실시간 (WebSocket)

`wss://{도메인}/api/v1/ws`, 연결할 때 `Authorization: Bearer {accessToken}` 헤더. 메시지는 JSON `{ "type": "...", "data": { ... } }`.

| 방향 | type | data |
| --- | --- | --- |
| 앱 → 서버 | `ping` | `{}` → `pong` |
| 앱 → 서버 | `presence.update` | `{ status, icon }` (`PUT /api/v1/presence`와 같아요) |
| 앱 → 서버 | `squish.ack` | `{ squishIds }` (SQSH-2) |
| 서버 → 앱 | `token.expiring` | `{ expiresAt }` 만료 1분 전 |
| 서버 → 앱 | `presence.changed` · `friend.workStarted` · `squish.received` · `squish.pending` · `friend.added` · `friend.removed` | 각 기능에서 |
| 서버 → 앱 | `error` | `{ code }` (`MALFORMED_MESSAGE`, `UNKNOWN_TYPE`, `INVALID_PRESENCE`) |

## 연결 수명

- 30초 동안 아무 메시지가 없으면 서버가 끊어요 (닫힘 코드 4000). 앱은 하트비트(`presence.update`)나 `ping`을 보내 유지해요.
- Access Token 만료 1분 전에 `token.expiring`을 보내고, 만료되면 끊어요 (닫힘 코드 4001). 앱은 재발급 후 다시 연결해요.
- 끊기면 앱은 1초, 2초, 4초 … 최대 60초 간격으로 다시 붙어요.
- Nginx는 `Upgrade` 헤더를 넘기고 `proxy_read_timeout 75s`예요 (서버의 30초 유휴 끊기보다 길어요).

## 왜 이렇게

- **STOMP 없이**: Spring 쪽은 편하지만 Swift에 쓸 만한 STOMP 클라이언트가 마땅치 않아요. 메시지 종류가 몇 개 안 돼서 `URLSessionWebSocketTask`로 바로 붙는 순수 WebSocket이 더 단순해요.
- **인증은 다른 API와 같게**: 핸드셰이크 요청을 Spring Security(Resource Server)가 검증하고, 인터셉터는 결과(사용자 id·세션 id·만료)만 세션에 붙여요.
- **`UserMessageSender` 뒤에 숨기기**: 지금은 이 서버의 연결에만 보내요. 서버가 여러 대가 되면 Redis Pub/Sub으로 다른 서버에 넘기는 구현으로 바꾸고, 보내는 쪽(꾹, 상태 변경) 코드는 그대로 둬요.
- **한 세션에 동시에 쓰지 않기**: 꾹 API, 배치 등 여러 스레드가 같은 세션에 쓸 수 있어서 `ConcurrentWebSocketSessionDecorator`로 감싸요.
- **유휴 끊기를 핸들러가 직접**: 컨테이너 설정(`ServletServerContainerFactoryBean`)은 실제 서블릿 WebSocket 컨테이너가 있어야 만들어져서 MockMvc 테스트가 뜨지 않아요. 마지막 메시지 시각을 세션에 남기고 10초마다 확인해요.
- **지표**: `tomochi_websocket_sessions` (지금 연결 수).
