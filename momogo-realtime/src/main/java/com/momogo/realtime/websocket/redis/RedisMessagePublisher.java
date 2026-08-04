package com.momogo.realtime.websocket.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.RealtimeErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

  private final StringRedisTemplate redisTemplate;
  private final ChannelTopic roomTopic;
  private final ObjectMapper objectMapper;

  /**
   * 메시지를 JSON으로 직렬화하여 Redis 채널로 Publish 합니다.
   * @param messagePayload
   */
  public void publish(Object messagePayload) {

    String jsonMessage;

    try {
      jsonMessage = objectMapper.writeValueAsString(messagePayload);
    } catch (JsonProcessingException e) {
      log.error("[Redis Publisher] JSON 직렬화 실패", e);
      throw new BusinessException(RealtimeErrorCode.JSON_SERIALIZATION_FAILED);
    }

    // 2. Redis 메시지 발행 수행 (실패 시 503 REDIS_PUBLISH_FAILED)
    try {
      log.debug("[Redis Publisher] Redis 채널({})로 메시지 발행: {}", roomTopic.getTopic(), jsonMessage);

      Long receiversCount = redisTemplate.convertAndSend(roomTopic.getTopic(), jsonMessage);

      if (receiversCount == null || receiversCount == 0) {
        log.warn("[Redis Publisher] 수신자 0명 - 채널({})로 발행되었으나 메시지를 수신한 구독자/인스턴스가 없습니다.", roomTopic.getTopic());
      } else {
        log.info("[Redis Publisher] Redis 채널({}) 메시지 발행 성공 (수신 인스턴스: {}개)", roomTopic.getTopic(), receiversCount);
      }
    } catch (Exception e) {
      log.error("[Redis Publisher] Redis 메시지 전송 실패 - topic: {}", roomTopic.getTopic(), e);
      throw new BusinessException(RealtimeErrorCode.REDIS_PUBLISH_FAILED);
    }
  }

}
