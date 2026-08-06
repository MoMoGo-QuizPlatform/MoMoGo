package com.momogo.api.notification.redis;

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
 *
 * 역직렬화는 MessageListenerAdapter에 등록한 Jackson2JsonRedisSerializer가 대신 처리하므로
 * 여기서는 이미 NotificationSseMessage 객체로 전달받는다. 역직렬화 자체가 실패하면
 * RedisMessageListenerContainer의 errorHandler가 잡아서 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

  private final NotificationEmitterRegistry emitterRegistry;

  public void handleMessage(NotificationSseMessage message) {
    if (message == null || message.userId() == null || message.notification() == null
        || message.notification().id() == null) {
      log.warn("[NotificationRedisSubscriber] 필수 필드가 없는 Redis 메시지를 무시합니다 - message: {}", message);
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
