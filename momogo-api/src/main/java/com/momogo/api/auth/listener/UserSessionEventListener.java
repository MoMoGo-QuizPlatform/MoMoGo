package com.momogo.api.auth.listener;

import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.core.common.config.AsyncConfig;
import com.momogo.core.common.util.EmailFormatter;
import com.momogo.core.domain.user.event.PasswordChangedEvent;
import com.momogo.core.domain.user.event.UserBannedEvent;
import com.momogo.core.domain.user.event.UserCacheEvictEvent;
import com.momogo.core.domain.user.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessionEventListener {

    private static final String USER_DTO_CACHE = "user_dtos";

    private final JwtRegistry jwtRegistry;
    private final RedisCacheManager cacheManager;

    // fallbackExecution = true 옵션을 명시하여 트랜잭션이 없을 때도 즉시 이벤트가 정상적으로 처리되도록 안전만 구축
    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("[UserSessionEventListener] 유저 정지 이벤트 감지 - userId: {}", event.userId());
        invalidateSession(event.userId(), "정지");
    }

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        log.info("[UserSessionEventListener] 유저 탈퇴 이벤트 감지 - userId: {}", event.userId());
        invalidateSession(event.userId(), "탈퇴");
    }

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePasswordChangedEvent(PasswordChangedEvent event) {
        log.info("[UserSessionEventListener] 비밀번호 변경 이벤트 감지 - userId: {}", event.userId());
        invalidateSession(event.userId(), "비밀번호 변경");
    }

    private void invalidateSession(UUID userId, String eventName) {
        try {
            jwtRegistry.invalidateJwtInformationByUserId(userId);
            log.info("[UserSessionEventListener] {} 처리로 인한 유저의 모든 JWT 세션이 무효화되었습니다. userId: {}", eventName, userId);
        } catch (Exception e) {
            log.error("[UserSessionEventListener] JWT 세션 무효화 실패, event: {}  userId: {}", eventName, userId, e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserCacheEvictEvent(UserCacheEvictEvent event) {
        if (event == null || event.email() == null) {
            return;
        }
        String normalizedEmail = EmailFormatter.normalize(event.email());
        log.info("[UserSessionEventListener] 유저 캐시 무효화 이벤트 감지 - email: {}", EmailFormatter.mask(normalizedEmail));

        try {
            Cache cache = cacheManager.getCache(USER_DTO_CACHE);
            if (cache != null) {
                cache.evict(normalizedEmail);
            } else {
                log.warn("[UserSessionEventListener] {} 캐시를 찾을 수 없어 evict를 건너뜁니다.", USER_DTO_CACHE);
            }
        } catch (Exception e) {
            log.error("[UserSessionEventListener] Redis 캐시 무효화 중 예외 발생 - email: {}", EmailFormatter.mask(normalizedEmail), e);
        }
    }
}
