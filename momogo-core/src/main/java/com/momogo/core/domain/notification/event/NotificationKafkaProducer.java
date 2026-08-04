package com.momogo.core.domain.notification.event;

import com.momogo.core.common.config.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 * 알림 이벤트를 notification-events 토픽에 발행하는 전담 클래스.
 * 실제 DB 저장/SSE 전송은 momogo-api의 NotificationKafkaConsumer가 비동기로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void send(NotificationEventMessage message) {
    kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, message)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("[NotificationKafkaProducer] 카프카 메시지 전송 실패 - eventId: {}, topic: {}",
                message.eventId(), KafkaTopics.NOTIFICATION_EVENTS, ex);
          } else {
            log.info("[NotificationKafkaProducer] 카프카 메시지 전송 성공 - eventId: {}, offset: {}",
                message.eventId(), result.getRecordMetadata().offset());
          }
        });
  }
}
