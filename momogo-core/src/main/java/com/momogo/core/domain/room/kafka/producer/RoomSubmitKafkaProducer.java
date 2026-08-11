package com.momogo.core.domain.room.kafka.producer;

import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.room.event.RoomSubmitEventMessage;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/*
 * 답안 제출 이벤트를 room-submit-events 토픽에 발행하는 전담 클래스.
 * 카프카 브로커의 발행 확정(ACK)을 타임아웃 내에 동기적으로 대기하여 내구성을 보장하며,
 * 실제 DB 저장은 momogo-api의 RoomSubmitKafkaConsumer가 비동기로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSubmitKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void send(RoomSubmitEventMessage message) {
    try {
      SendResult<String, Object> result = kafkaTemplate.send(
          KafkaTopics.ROOM_SUBMIT_EVENTS,
          message.roomId().toString(),
          message
      ).get(3, TimeUnit.SECONDS);

      log.info("[RoomSubmitKafkaProducer] 카프카 메시지 전송 성공 - eventId: {}, offset: {}",
          message.eventId(), result.getRecordMetadata().offset());
    } catch (Exception ex) {
      log.error("[RoomSubmitKafkaProducer] 카프카 메시지 전송 실패 - eventId: {}, roomId: {}, topic: {}",
          message.eventId(), message.roomId(), KafkaTopics.ROOM_SUBMIT_EVENTS, ex);
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "답안 제출 전송 실패");
    }
  }
}
