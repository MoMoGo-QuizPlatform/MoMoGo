package com.momogo.api.notification.service;

import com.momogo.api.notification.registry.NotificationEmitterRegistry;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.sse.NotificationSseService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * NotificationSseService(publish) + NotificationSseConnector(connect) 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseServiceImpl implements NotificationSseService, NotificationSseConnector {

  // SSE 연결 최대 유지 시간
  private static final long TIMEOUT = 30L * 60 * 1000; // 30분

  private final NotificationEmitterRegistry emitterRegistry;

  // 클라이언트의 최초 SSE 연결 요청을 처리
  @Override
  public SseEmitter connect(UUID userId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT);
    emitterRegistry.register(userId, emitter);

    // 연결 직후 더미 이벤트를 하나 보내는 이유:
    // 클라이언트가 아무 데이터도 안 보내고 가만히 있으면 연결이 대기상태로 오해받아 타임아웃/503이 날 수 있음
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected"));
    } catch (IOException e) {
      log.warn("SSE 초기 연결 이벤트 전송 실패 - userId: {}", userId, e);
      // completeWithError()가 emitter의 onError 콜백을 트리거 -> registry 정리
      emitter.completeWithError(e);
    }

    return emitter;
  }

  // 실제 알림을 SSE로 전송
  @Override
  public void publish(UUID userId, NotificationResponse notification) {
    for (SseEmitter emitter : emitterRegistry.findAllByUserId(userId)) {
      try {
        emitter.send(SseEmitter.event()
            .id(notification.id().toString())
            .name("notifications")
            .data(notification));
      } catch (IOException e) {
        log.warn("SSE 알림 전송 실패 - userId: {}", userId, e);
        // completeWithError()가 emitter의 onError 콜백을 트리거 -> registry 정리
        emitter.completeWithError(e);
      }
    }
  }
}
