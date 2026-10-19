package com.tomochi.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 정리·집계 배치({@code @Scheduled})를 켜요. 테스트에서는 {@code tomochi.scheduling.enabled=false}로 꺼요.
 * 서버가 여러 대가 되면 같은 배치가 대수만큼 돌아요. 지금 배치는 몇 번 돌아도 결과가 같게(멱등) 만들어요.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "tomochi.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
