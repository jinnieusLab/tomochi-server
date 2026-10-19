package com.tomochi.realtime;

import tools.jackson.databind.JsonNode;

/**
 * WebSocket 메시지 한 개. {@code { "type": "squish.received", "data": { ... } }}
 *
 * <pre>
 * 앱 → 서버: ping, presence.update, squish.ack
 * 서버 → 앱: pong, presence.changed, friend.workStarted, squish.received, squish.pending,
 *            friend.added, friend.removed, token.expiring, error
 * </pre>
 */
public record RealtimeMessage(String type, JsonNode data) {
}
