package com.momogo.api.auth.security;

import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.common.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * BCrypt 연산(encode/matches)의 동시 실행 개수를 물리 CPU 코어 수 수준으로 제한하는 PasswordEncoder 래퍼입니다.
 * <p>
 * 1. <b>배경:</b> 가상 스레드 환경에서 CPU 소모가 큰 BCrypt 연산이 동시에 몰리면 OS 컨텍스트 스위칭으로 인해 응답 지연이 치솟는 현상이 발생합니다.<br>
 * 2. <b>해결:</b> {@link Semaphore}로 물리 CPU 코어 수만큼만 BCrypt 동시 연산을 허용하고, 대기 시간 초과 요청은 {@link AuthErrorCode#LOGIN_SERVER_BUSY}(HTTP 503)로 빠르게 차단(Fail-Fast)합니다.<br>
 * 3. <b>적용:</b> 기존 PasswordEncoder 빈을 대체하여 자동 주입되며, 로직 수정 없이 시스템의 회복성과 응답 지연을 안정적으로 통제합니다.
 */
@Slf4j
@Component
public class BoundedBCryptPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate;
    private final Semaphore semaphore;
    private final long acquireTimeoutMs;
    private final int maxConcurrency;

    public BoundedBCryptPasswordEncoder(
            @Value("${app.security.bcrypt.cost:10}") int bcryptCost,
            @Value("${app.security.bcrypt.max-concurrency:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}") int maxConcurrency,
            @Value("${app.security.bcrypt.acquire-timeout-ms:3000}") long acquireTimeoutMs,
            MeterRegistry meterRegistry
    ) {
        this.delegate = new BCryptPasswordEncoder(bcryptCost);
        this.maxConcurrency = maxConcurrency;
        // fair=true: 먼저 대기한 요청이 먼저 슬롯을 얻도록 보장 (FIFO 큐잉, 기아 상태 방지)
        this.semaphore = new Semaphore(maxConcurrency, true);
        this.acquireTimeoutMs = acquireTimeoutMs;

        meterRegistry.gauge("auth.bcrypt.available.permits", semaphore, Semaphore::availablePermits);
        meterRegistry.gauge("auth.bcrypt.queue.length", semaphore, Semaphore::getQueueLength);

        log.info("[BoundedBCryptPasswordEncoder] 초기화 완료 - bcryptCost={}, maxConcurrency={}, acquireTimeoutMs={}ms",
                bcryptCost, maxConcurrency, acquireTimeoutMs);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return runBounded(() -> delegate.encode(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return runBounded(() -> delegate.matches(rawPassword, encodedPassword));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }

    private <T> T runBounded(Supplier<T> operation) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("[BoundedBCryptPasswordEncoder] BCrypt 슬롯 획득 타임아웃 발생 - " +
                                "maxConcurrency={}, 대기 큐 길이={}, 대기시간={}ms 초과",
                        maxConcurrency, semaphore.getQueueLength(), acquireTimeoutMs);
                throw new BusinessException(
                        AuthErrorCode.LOGIN_SERVER_BUSY,
                        "로그인 요청이 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해주세요."
                );
            }
            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[BoundedBCryptPasswordEncoder] BCrypt 슬롯 대기 중 인터럽트 발생", e);
            throw new BusinessException(
                    AuthErrorCode.LOGIN_SERVER_BUSY,
                    "로그인 처리 중 인터럽트가 발생했습니다.",
                    e
            );
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
}