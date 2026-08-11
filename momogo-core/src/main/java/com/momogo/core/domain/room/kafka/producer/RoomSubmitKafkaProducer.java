package com.momogo.core.domain.room.kafka.producer;

import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.domain.room.event.RoomSubmitEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 * 답안 제출 이벤트를 room-submit-events 토픽에 비동기 발행하는 전담 클래스.
 * 실제 DB 저장은 momogo-api의 RoomSubmitKafkaConsumer가 비동기로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSubmitKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void send(RoomSubmitEventMessage message) {
    kafkaTemplate.send(KafkaTopics.ROOM_SUBMIT_EVENTS, message.roomId().toString(), message)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("[RoomSubmitKafkaProducer] 카프카 메시지 전송 실패 - eventId: {}, roomId: {}, topic: {}",
                message.eventId(), message.roomId(), KafkaTopics.ROOM_SUBMIT_EVENTS, ex);
          } else {
            log.info("[RoomSubmitKafkaProducer] 카프카 메시지 전송 성공 - eventId: {}, offset: {}",
                message.eventId(), result.getRecordMetadata().offset());
          }
        });
  }
}
