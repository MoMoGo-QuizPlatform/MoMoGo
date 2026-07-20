package com.momogo.api.auth.listener;

import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.core.common.config.AsyncConfig;
import com.momogo.core.domain.user.event.PasswordChangedEvent;
import com.momogo.core.domain.user.event.UserBannedEvent;
import com.momogo.core.domain.user.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessionEventListener {

    private final JwtRegistry jwtRegistry;

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
}
