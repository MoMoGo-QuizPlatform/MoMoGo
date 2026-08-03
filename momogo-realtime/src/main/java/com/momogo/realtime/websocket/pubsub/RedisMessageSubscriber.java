package com.momogo.realtime.websocket.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * RedisPubSubConfig의 MessageListenerAdapter에 의해 호출되는 메소드
   * Redis에서 발행된 메시지를 받아 웹소켓 구독자(/sub/...)들에게 전파합니다.
   * @param messageJson
   */
  public  void handleMessage(String messageJson) {
    try {
      log.info("[Redis Subscriber] 수신된 Pub/Sub 메시지: {}", messageJson);

      // TODO: JSON 메시지를 DTO로 직렬화해서 목적지(/sub/rooms/{roomId})로 웹소켓 브로드캐스팅
    } catch (Exception e) {
      log.error("[Redis Subscriber] 메시지 처리 중 오류 발생", e);
    }
  }

}
