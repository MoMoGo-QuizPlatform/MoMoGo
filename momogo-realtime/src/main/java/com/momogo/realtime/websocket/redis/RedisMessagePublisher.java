package com.momogo.realtime.websocket.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.RealtimeErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

  private final RedisTemplate<String, Object> redisTemplate;
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
      log.error("[Redis Publisher] JSON 직렬화 실패 - payload: {}", messagePayload, e);
      throw new BusinessException(RealtimeErrorCode.JSON_SERIALIZATION_FAILED);
    }

    // 2. Redis 메시지 발행 수행 (실패 시 503 REDIS_PUBLISH_FAILED)
    try {
      log.info("[Redis Publisher] Redis 채널({})로 메시지 발행: {}", roomTopic.getTopic(), jsonMessage);
      redisTemplate.convertAndSend(roomTopic.getTopic(), jsonMessage);
    } catch (Exception e) {
      log.error("[Redis Publisher] Redis 메시지 전송 실패 - payload: {}", messagePayload, e);
      throw new BusinessException(RealtimeErrorCode.REDIS_PUBLISH_FAILED);
    }
  }

}
