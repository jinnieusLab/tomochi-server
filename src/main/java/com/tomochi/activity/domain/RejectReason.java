package com.tomochi.activity.domain;

/** 구간 하나를 받지 않은 이유. 앱은 이 구간을 다시 보내지 않고 버려요. */
public enum RejectReason {
	/** clientSegmentId가 UUID가 아니에요 */
	INVALID_ID,
	/** 시작·끝이 없거나, 끝이 시작보다 앞이에요 */
	INVALID_RANGE,
	/** 시작부터 미래예요 */
	IN_FUTURE,
	/** 시작이 7일보다 전이에요 (오프라인으로 쌓아 둘 수 있는 범위를 넘음) */
	TOO_OLD,
	/** 12시간보다 길어요 */
	TOO_LONG
}
