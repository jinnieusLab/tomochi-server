package com.tomochi.activity.controller;

import com.tomochi.activity.dto.UploadSegmentsRequest;
import com.tomochi.activity.dto.UploadSegmentsResponse;
import com.tomochi.activity.service.ActivitySegmentService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activity")
@RequiredArgsConstructor
public class ActivityController implements ActivityApi {

	private final ActivitySegmentService activitySegmentService;

	@Override
	@PostMapping("/segments")
	public UploadSegmentsResponse upload(@CurrentUser AuthUser user, @Valid @RequestBody UploadSegmentsRequest request) {
		return activitySegmentService.upload(user.userId(), request);
	}
}
