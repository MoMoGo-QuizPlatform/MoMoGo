package com.momogo.core.common.config;

import com.momogo.core.common.exception.GlobalAsyncUncaughtExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 애플리케이션 비동기 처리(가상 스레드 기반 Executor) 설정 클래스.
 * 가상 스레드(Virtual Thread) 환경에서도 외부 I/O(S3, SMTP, DB) 및 Connection Pool 자원 고갈을 방지하기 위해
 * SimpleAsyncTaskExecutor에 Executor별 동시성 상한(Concurrency Limit)을 설정합니다.
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

    @Value("${app.async.concurrency-limit.default}")
    private int defaultConcurrencyLimit;

    @Value("${app.async.concurrency-limit.mail}")
    private int mailConcurrencyLimit;

    @Value("${app.async.concurrency-limit.user}")
    private int userConcurrencyLimit;

    @Value("${app.async.concurrency-limit.file}")
    private int fileConcurrencyLimit;

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
        return createVirtualThreadExecutor("default-async-", defaultConcurrencyLimit);
    }

    /**
     * 메일 발송 전용 비동기 Executor
     */
    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        return createVirtualThreadExecutor("mail-async-", mailConcurrencyLimit);
    }

    /**
     * 사용자 이벤트 처리 전용 비동기 Executor
     */
    @Bean(name = USER_EXECUTOR)
    public Executor userExecutor() {
        return createVirtualThreadExecutor("user-async-", userConcurrencyLimit);
    }

    /**
     * 파일/Storage 처리 전용 비동기 Executor
     */
    @Bean(name = FILE_EXECUTOR)
    public Executor fileExecutor() {
        return createVirtualThreadExecutor("file-async-", fileConcurrencyLimit);
    }

    /**
     * 가상 스레드 기반 Executor 생성 공통 팩토리 메서드 (DRY 원칙 및 동시성 상한 적용)
     */
    private Executor createVirtualThreadExecutor(String threadNamePrefix, int concurrencyLimit) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(concurrencyLimit);
        taskDecoratorProvider.ifAvailable(executor::setTaskDecorator);
        return executor;
    }
}
