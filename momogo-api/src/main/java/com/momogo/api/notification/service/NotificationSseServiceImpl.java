package com.momogo.api.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.api.notification.redis.NotificationSseMessage;
import com.momogo.api.notification.registry.NotificationEmitterRegistry;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.sse.NotificationSseService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * NotificationSseService(publish) + NotificationSseConnector(connect) 구현체.
 *
 * publish()는 로컬 emitter에 직접 쓰지 않고 Redis 채널로 발행만 한다. 실제 로컬 전송은
 * NotificationRedisSubscriber가 이 채널을 구독해서 담당한다 - 이 인스턴스에 연결된 유저가
 * 아니면 조용히 무시되므로, 어느 인스턴스가 publish()를 호출하든 상관없이 그 유저가
 * 실제로 연결된 인스턴스에서 SSE가 전송된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseServiceImpl implements NotificationSseService, NotificationSseConnector {

  // SSE 연결 최대 유지 시간
  private static final long TIMEOUT = 30L * 60 * 1000; // 30분

  private final NotificationEmitterRegistry emitterRegistry;
  private final StringRedisTemplate stringRedisTemplate;
  private final ChannelTopic notificationTopic;
  private final ObjectMapper objectMapper;

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
      // 이 시점 실패는 서블릿 컨테이너의 오류 디스패치로 정리되므로 로그만 남김
      log.warn("SSE 초기 연결 이벤트 전송 실패 - userId: {}", userId, e);
    }

    return emitter;
  }

  // 알림을 Redis 채널로 발행 (실제 SSE 전송은 NotificationRedisSubscriber가 수행)
  // 발행 실패는 여기서 삼키지 않고 그대로 던진다. 호출자(NotificationSsePublishListener,
  // NotificationEventListener)가 이미 유저 단위로 try/catch하고 있어, 한 명의 발행 실패가
  // 다른 유저들의 알림 처리를 막지 않으면서도 실패를 호출자가 인지할 수 있다.
  @Override
  public void publish(UUID userId, NotificationResponse notification) {
    String json;
    try {
      json = objectMapper.writeValueAsString(new NotificationSseMessage(userId, notification));
    } catch (JsonProcessingException e) {
      throw new BusinessException(GlobalErrorCode.SSE_PUBLISH_FAILED, "SSE 메시지 직렬화 실패 - userId: " + userId, e);
    }

    Long receivers = stringRedisTemplate.convertAndSend(notificationTopic.getTopic(), json);
    log.info("[NotificationSseServiceImpl] Redis 채널({}) 발행 완료 - userId: {}, 구독 중인 인스턴스 수: {}",
        notificationTopic.getTopic(), userId, receivers);
  }
}
