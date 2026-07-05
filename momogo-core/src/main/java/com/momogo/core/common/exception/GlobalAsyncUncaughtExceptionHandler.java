package com.momogo.core.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 반환 타입이 void인 비동기(@Async) 메서드 내에서 발생한 예외를 처리하는 클래스입니다.
 * void 비동기 메서드는 예외가 발생하더라도 호출한 측에서 에러 여부를 확인할 수 없기 때문에,
 * 백그라운드 스레드에서 던져진 예외를 전역적으로 포착하고 공통 로그 처리 및 예외 복구를 수행하기 추가하였습니다.
 */
@Slf4j
@Component
public class GlobalAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("[Async Error] 비동기 처리 중 예외 발생 - Method: {}, Exception: {}",
                method.getName(), ex.getMessage(), ex);
    }
}
