package com.tomochi.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** WebSocket 세션의 토큰 만료 알림·끊기를 예약하는 스케줄러. 배치용 스케줄러와 따로 둬요. */
@Configuration
public class RealtimeSchedulerConfig {

	public static final String REALTIME_SCHEDULER = "realtimeTaskScheduler";

	@Bean(REALTIME_SCHEDULER)
	public ThreadPoolTaskScheduler realtimeTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("realtime-");
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}
}
