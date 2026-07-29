package com.momogo.realtime.config;

import com.momogo.core.domain.notification.sse.NotificationSseService;
import com.momogo.core.domain.problem.service.ProblemGenerationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Realtime 모듈에는 없는 core 필수 의존성의 대체(Fallback) 빈 모음.
 * Realtime 앱은 core 전체를 컴포넌트 스캔하는데, core의 일부 서비스가 요구하는 구현체
 */
@Configuration
public class RealtimeFallbackConfig {

    /**
     * core NotificationEventListener가 요구. Realtime 중 알림 이벤트는 발생할 수 있으나
     * 실시간 전송 대상(접속 클라이언트)이 없으므로 no-op이 올바른 동작.
     */
    @Bean
    @ConditionalOnMissingBean(NotificationSseService.class)
    public NotificationSseService noopNotificationSseService() {
        return (userId, notification) -> {
        };
    }

    /**
     * core ProblemServiceImpl 등이 요구. Realtime에서 AI 문제 생성이 호출될 일은 없어야 하므로 예외.
     */
    @Bean
    @ConditionalOnMissingBean(ProblemGenerationService.class)
    public ProblemGenerationService dummyProblemGenerationService() {
        return (referenceText, questionCount) -> {
            throw new UnsupportedOperationException("Realtime 모듈에서는 AI 문제 생성을 지원하지 않습니다.");
        };
    }
}
