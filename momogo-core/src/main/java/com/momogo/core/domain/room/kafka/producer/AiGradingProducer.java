package com.momogo.core.domain.room.kafka.producer;

import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.domain.room.dto.event.AiGradingEventDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiGradingProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendAiGradingEvent(UUID roomId, UUID userId) {
    AiGradingEventDto eventDto = AiGradingEventDto.of(roomId, userId);
    log.info("[AiGradingProducer] Kafka AI 채점 요청 발행 - roomId: {}, userId: {}", roomId, userId);

    kafkaTemplate.send(KafkaTopics.AI_GRADING_EVENTS, roomId.toString(), eventDto);
  }

}
