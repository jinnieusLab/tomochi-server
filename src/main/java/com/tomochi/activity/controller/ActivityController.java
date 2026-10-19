package com.tomochi.activity.controller;

import com.tomochi.activity.dto.UploadSegmentsRequest;
import com.tomochi.activity.dto.UploadSegmentsResponse;
import com.tomochi.activity.service.ActivitySegmentService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "활동 기록")
@RestController
@RequestMapping("/api/v1/activity")
@RequiredArgsConstructor
public class ActivityController {

	private final ActivitySegmentService activitySegmentService;

	@Operation(summary = "작업 구간 올리기", description = """
			앱은 5분마다, 그리고 끌 때 작업 구간을 모아 보내요 (한 번에 200개까지).
			열린 구간은 "지금까지"로 잘라 보내고, 다음에 같은 `clientSegmentId`로 늘려 보내면 돼요.
			같은 요청을 여러 번 보내도 시간이 두 번 더해지지 않아요.
			- 끝이 서버 시각 + 2분보다 뒤면 서버 시각으로 잘라요 (`adjusted`)
			- 끝 ≤ 시작, 7일보다 오래됨, 12시간 초과는 그 구간만 거절해요 (`rejected`)""")
	@PostMapping("/segments")
	public UploadSegmentsResponse upload(@CurrentUser AuthUser user, @Valid @RequestBody UploadSegmentsRequest request) {
		return activitySegmentService.upload(user.userId(), request);
	}
}
