package com.momogo.core.common.config;

import com.momogo.core.common.exception.GlobalAsyncUncaughtExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    public static final String MAIL_EXECUTOR = "mailExecutor";

    private final GlobalAsyncUncaughtExceptionHandler exceptionHandler;

    @Override
    public Executor getAsyncExecutor() {
        return mailExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return exceptionHandler;
    }

    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("mail-async-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
