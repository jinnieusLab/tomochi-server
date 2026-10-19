package com.tomochi.activity.service;

import com.tomochi.activity.domain.SegmentPolicy;
import com.tomochi.activity.domain.TimeRange;
import com.tomochi.activity.dto.SegmentRequest;
import com.tomochi.activity.dto.UploadSegmentsRequest;
import com.tomochi.activity.dto.UploadSegmentsResponse;
import com.tomochi.activity.dto.UploadSegmentsResponse.Rejected;
import com.tomochi.activity.repository.ActivitySegmentWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivitySegmentService {

	private final ActivitySegmentWriter activitySegmentWriter;
	private final Clock clock;

	/**
	 * 구간마다 따로 검증해서, 틀린 구간이 있어도 나머지는 저장해요.
	 * 오프라인으로 쌓인 구간 중 하나 때문에 전부 막히면 앱이 영영 올리지 못해요.
	 */
	@Transactional
	public UploadSegmentsResponse upload(Long userId, UploadSegmentsRequest request) {
		Instant now = clock.instant();
		Map<String, TimeRange> accepted = new LinkedHashMap<>();
		List<Rejected> rejected = new ArrayList<>();
		int adjusted = 0;

		for (SegmentRequest segment : request.segments()) {
			SegmentPolicy.Result result =
					SegmentPolicy.check(segment.clientSegmentId(), segment.start(), segment.end(), now);
			if (!result.isAccepted()) {
				rejected.add(new Rejected(segment.clientSegmentId(), result.rejectReason()));
				continue;
			}
			if (result.adjusted()) {
				adjusted++;
			}
			// 한 요청에 같은 id가 두 번 오면 DB와 같은 규칙(넓히기)으로 미리 합쳐요.
			accepted.merge(result.clientSegmentId(), result.range(), ActivitySegmentService::widen);
		}

		if (!accepted.isEmpty()) {
			activitySegmentWriter.upsert(userId, accepted, now);
		}
		return new UploadSegmentsResponse(request.segments().size() - rejected.size(), adjusted, rejected);
	}

	private static TimeRange widen(TimeRange a, TimeRange b) {
		return new TimeRange(
				a.start().isBefore(b.start()) ? a.start() : b.start(),
				a.end().isAfter(b.end()) ? a.end() : b.end());
	}
}
