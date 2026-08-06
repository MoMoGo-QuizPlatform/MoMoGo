package com.momogo.api.notification.scheduler;

import com.momogo.api.notification.registry.NotificationEmitterRegistry;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * SSE 연결이 오래 idle 상태면 중간 프록시/로드밸런서가 죽은 연결로 오해해 끊어버릴 수 있음
 * 일정 주기로 빈 코멘트를 보내 연결이 살아있음을 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseHeartbeatScheduler {

  private static final long HEARTBEAT_INTERVAL = 30_000L; // 30초

  private final NotificationEmitterRegistry emitterRegistry;

  @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
  public void sendHeartbeat() {
    for (SseEmitter emitter : emitterRegistry.findAll()) {
      try {
        emitter.send(SseEmitter.event().comment("heartbeat"));
      } catch (IOException | IllegalStateException e) {
        log.warn("[NotificationSseHeartbeatScheduler] 하트비트 전송 실패, 연결 정리", e);
        emitter.completeWithError(e);
      }
    }
  }
}
