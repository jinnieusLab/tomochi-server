package com.tomochi.activity.dto;

import java.time.Instant;

/**
 * @param clientSegmentId 앱이 구간마다 만든 UUID. 같은 구간을 늘려 다시 보낼 때도 같은 값을 써요
 * @param start           작업 중이 시작된 시각 (UTC)
 * @param end             마지막 입력 시각. 열린 구간이면 "지금까지"
 */
public record SegmentRequest(String clientSegmentId, Instant start, Instant end) {
}
