package com.momogo.realtime.websocket.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

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
    try {
      String jsonMessage = objectMapper.writeValueAsString(messagePayload);
      log.info("[Redis Publisher] Redis 채널({})로 메시지 발행: {}", roomTopic.getTopic(), jsonMessage);

      redisTemplate.convertAndSend(roomTopic.getTopic(), jsonMessage);

    } catch (Exception e) {
      log.error("[Redis Publisher] 메시지 발행 실패", e);
    }
  }

}
