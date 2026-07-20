package com.momogo.core.common.config;

import com.momogo.core.common.exception.GlobalAsyncUncaughtExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 애플리케이션 비동기 처리(가상 스레드 기반 Executor) 설정 클래스.
 * TaskDecorator 빈이 존재하는 경우, 비동기 스레드 실행 시 컨텍스트(MDC, SecurityContext) 전파를 설정합니다.
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    public static final String DEFAULT_EXECUTOR = "defaultExecutor";
    public static final String MAIL_EXECUTOR = "mailExecutor";
    public static final String USER_EXECUTOR = "userExecutor";

    private final GlobalAsyncUncaughtExceptionHandler exceptionHandler;
    private final ObjectProvider<TaskDecorator> taskDecoratorProvider;

    @Override
    public Executor getAsyncExecutor() {
        return defaultExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return exceptionHandler;
    }

    @Bean(name = DEFAULT_EXECUTOR)
    public Executor defaultExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("default-async-");
        executor.setVirtualThreads(true);
        taskDecoratorProvider.ifAvailable(executor::setTaskDecorator);
        return executor;
    }

    /**
     * 메일 발송 전용 비동기 Executor
     */
    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("mail-async-");
        executor.setVirtualThreads(true);
        taskDecoratorProvider.ifAvailable(executor::setTaskDecorator);
        return executor;
    }

    /**
     * 사용자 이벤트 처리 전용 비동기 Executor
     */
    @Bean(name = USER_EXECUTOR)
    public Executor userExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("user-async-");
        executor.setVirtualThreads(true);
        taskDecoratorProvider.ifAvailable(executor::setTaskDecorator);
        return executor;
    }
}
