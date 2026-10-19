package com.tomochi.activity.dto;

import com.tomochi.activity.domain.RejectReason;
import java.util.List;

/**
 * @param accepted 저장한 구간 수 (잘라서 저장한 것 포함)
 * @param adjusted 미래 시각을 서버 시각으로 잘라 저장한 구간 수
 * @param rejected 받지 않은 구간. 앱은 다시 보내지 않고 버려요
 */
public record UploadSegmentsResponse(int accepted, int adjusted, List<Rejected> rejected) {

	public record Rejected(String clientSegmentId, RejectReason reason) {
	}
}
