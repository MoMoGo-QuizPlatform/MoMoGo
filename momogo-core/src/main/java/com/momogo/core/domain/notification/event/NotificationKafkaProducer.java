package com.momogo.core.domain.notification.event;

import com.momogo.core.common.config.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 * 알림 이벤트를 notification-events 토픽에 발행하는 전담 클래스.
 * 실제 DB 저장/SSE 전송은 momogo-api의 NotificationKafkaConsumer가 비동기로 처리
 */
@Component
@RequiredArgsConstructor
public class NotificationKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void send(NotificationEventMessage message) {
    kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, message);
  }
}
