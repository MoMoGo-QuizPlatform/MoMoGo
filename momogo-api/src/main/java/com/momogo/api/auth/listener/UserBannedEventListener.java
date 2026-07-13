package com.momogo.api.auth.listener;

import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.core.domain.user.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBannedEventListener {

    private final JwtRegistry jwtRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("[UserBannedEventListener] 유저 정지 이벤트 감지 - userId: {}", event.userId());
        jwtRegistry.invalidateJwtInformationByUserId(event.userId());
        log.info("[UserBannedEventListener] 정지된 유저의 모든 JWT 세션이 무효화되었습니다. userId: {}", event.userId());
    }
}
