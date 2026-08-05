package com.momogo.api.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.api.notification.registry.NotificationEmitterRegistry;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * NotificationRedisPubSubConfig의 MessageListenerAdapter가 호출하는 구독자.
 * Redis 채널로 발행된 알림을 받아, 이 인스턴스에 실제로 연결된 유저가 있으면 SSE로 전송
 * 연결된 유저가 없으면(다른 인스턴스에 연결된 유저면) 조용히 무시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

  private final ObjectMapper objectMapper;
  private final NotificationEmitterRegistry emitterRegistry;

  public void handleMessage(String messageJson) {
    NotificationSseMessage message;
    try {
      message = objectMapper.readValue(messageJson, NotificationSseMessage.class);
    } catch (IOException e) {
      log.error("[NotificationRedisSubscriber] 메시지 역직렬화 실패 - payload: {}", messageJson, e);
      return;
    }

    NotificationResponse notification = message.notification();
    var emitters = emitterRegistry.findAllByUserId(message.userId());
    if (emitters.isEmpty()) {
      log.info("[NotificationRedisSubscriber] Redis 메시지 수신 - userId: {} (이 인스턴스엔 연결 없음, 무시)",
          message.userId());
      return;
    }
    log.info("[NotificationRedisSubscriber] Redis 메시지 수신 - userId: {}, 이 인스턴스의 연결 수: {} -> SSE 전송",
        message.userId(), emitters.size());

    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event()
            .id(notification.id().toString())
            .name("notifications")
            .data(notification));
      } catch (IOException e) {
        log.warn("[NotificationRedisSubscriber] SSE 알림 전송 실패 - userId: {}", message.userId(), e);
        emitter.completeWithError(e);
      }
    }
  }
}
