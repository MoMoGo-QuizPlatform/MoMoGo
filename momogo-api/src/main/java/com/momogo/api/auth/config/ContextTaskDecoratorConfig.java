package com.momogo.api.auth.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * 비동기 스레드 실행 시 부모 스레드의 MDC(로그 컨텍스트) 및 SecurityContext(인증 컨텍스트)를 전파하기 위한 설정
 */
@Configuration
public class ContextTaskDecoratorConfig {

    /**
     * 부모 스레드의 MDC 및 SecurityContext를 캡처하여 비동기 스레드로 전파하고,
     * 작업 완료 후 컨텍스트를 안전하게 제거하는 TaskDecorator를 생성합니다.
     */
    @Bean
    public TaskDecorator contextTaskDecorator() {
        return runnable -> {
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            // 다중 스레드 환경이나 컨텍스트 홀더 저장 방식이 변경될 때 훨씬 유연하게 대응할 수 있음
            var strategy = SecurityContextHolder.getContextHolderStrategy();
            SecurityContext securityContext = strategy.getContext();

            return () -> {
                try {
                    if (mdcContext != null) MDC.setContextMap(mdcContext);
                    if (securityContext != null) strategy.setContext(securityContext);
                    runnable.run();
                } finally {
                    MDC.clear();
                    strategy.clearContext();
                }
            };
        };
    }
}
