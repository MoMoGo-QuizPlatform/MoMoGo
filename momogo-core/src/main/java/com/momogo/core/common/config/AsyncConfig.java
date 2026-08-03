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
 * 가상 스레드(Virtual Thread) 환경에서는 호출 스레드 블로킹 방지를 위해 무제한 동시성(Unbounded Concurrency)으로 동작시킵니다.
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    public static final String DEFAULT_EXECUTOR = "defaultExecutor";
    public static final String MAIL_EXECUTOR = "mailExecutor";
    public static final String USER_EXECUTOR = "userExecutor";
    public static final String FILE_EXECUTOR = "fileExecutor";

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
        return createVirtualThreadExecutor("default-async-");
    }

    /**
     * 메일 발송 전용 비동기 Executor
     */
    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        return createVirtualThreadExecutor("mail-async-");
    }

    /**
     * 사용자 이벤트 처리 전용 비동기 Executor
     */
    @Bean(name = USER_EXECUTOR)
    public Executor userExecutor() {
        return createVirtualThreadExecutor("user-async-");
    }

    /**
     * 파일/Storage 처리 전용 비동기 Executor
     */
    @Bean(name = FILE_EXECUTOR)
    public Executor fileExecutor() {
        return createVirtualThreadExecutor("file-async-");
    }

    /**
     * 가상 스레드 기반 Executor 생성 공통 팩토리 메서드 (DRY 원칙 적용)
     */
    private Executor createVirtualThreadExecutor(String threadNamePrefix) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
        executor.setVirtualThreads(true);
        taskDecoratorProvider.ifAvailable(executor::setTaskDecorator);
        return executor;
    }
}
