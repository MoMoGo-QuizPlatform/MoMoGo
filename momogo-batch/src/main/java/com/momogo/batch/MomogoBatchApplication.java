package com.momogo.batch;

import com.momogo.core.domain.notification.sse.NotificationSseService;
import com.momogo.core.domain.problem.service.ProblemGenerationService;
import com.momogo.core.domain.user.service.RestoreTokenValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.momogo")
@EntityScan(basePackages = "com.momogo.core.domain")
@EnableJpaRepositories(basePackages = "com.momogo.core.domain")
public class MomogoBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(MomogoBatchApplication.class, args);
    }

    /**
     * Batch 모듈 구동 시 core 모듈의 UserServiceImpl이 필수 주입을 원하므로
     * 더미 객체를 직접 등록하여 ConText 구동 실패를 방어
     */
    @Bean
    public RestoreTokenValidator dummyRestoreTokenValidator() {
        return token -> {
            throw new UnsupportedOperationException("Batch 모듈에서는 복구 토큰 검증을 지원하지 않습니다.");
        };
    }

    /**
     * core의 NotificationEventListener가 SSE 전송 빈을 필수 주입으로 요구하지만,
     * Batch 모듈에는 SSE 연결(클라이언트)이 존재하지 않으므로 아무것도 하지 않는 구현을 등록
     */
    @Bean
    public NotificationSseService noopNotificationSseService() {
        return (userId, notification) -> {
        };
    }

    /**
     * core의 ProblemServiceImpl 등이 AI 문제 생성 빈을 필수 주입으로 요구하지만,
     * 실제 구현체는 ai 모듈에 있고 Batch 모듈은 문제 생성을 수행하지 않으므로 더미를 등록
     */
    @Bean
    public ProblemGenerationService dummyProblemGenerationService() {
        return (referenceText, questionCount) -> {
            throw new UnsupportedOperationException("Batch 모듈에서는 AI 문제 생성을 지원하지 않습니다.");
        };
    }
}
