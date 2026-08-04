package com.momogo.api.notification.consumer;

import com.momogo.api.notification.event.NotificationsPersistedEvent;
import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.event.NotificationEventMessage;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import com.momogo.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * NotificationEventListener가 발행한 알림 메시지를 받아 실제 DB 저장을 수행
 * DB 저장 실패 등 진짜 예외는 그대로 던져서 KafkaConfig의 재시도/DLT 정책이 처리하게 함
 * SSE 전송은 여기서 하지 않고, 저장 완료 이벤트만 발행해 NotificationSsePublishListener가
 * 트랜잭션 커밋 이후에 처리하도록 위임한다 (커밋 실패 시 미저장 알림이 클라이언트로 새는 것 방지)
 *
 * 카프카는 최소 1회 전달을 보장하므로, 재전달로 같은 메시지가 두 번 도착할 수 있다.
 * eventId 기준으로 Redis에 처리 여부를 표시해 중복 저장/SSE 재발송을 막는다.
 *
 * userIds는 RoomServiceImpl.createRoom()에서 방 생성 시점에 이미 존재 검증을 마친 값이라,
 * 여기서는 별도 SELECT 없이 EntityManager.getReference()로 프록시 참조만 사용한다.
 * (그 사이 유저가 삭제되는 극단적인 경우는 saveAll() 시점 FK 위반으로 실패 -> 카프카 재시도/DLT로 격리됨)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

  private static final String DEDUP_KEY_PREFIX = "notification:processed:";
  private static final Duration DEDUP_TTL = Duration.ofHours(24);

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final RedisTemplate<String, Object> redisTemplate;
  private final EntityManager entityManager;

  @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS)
  @Transactional
  public void consume(NotificationEventMessage message) {
    log.info("[NotificationKafkaConsumer] 카프카 알림 수신 - type: {}, targetCount: {}",
        message.type(), message.userIds().size());

    Boolean isNew = redisTemplate.opsForValue()
        .setIfAbsent(DEDUP_KEY_PREFIX + message.eventId(), "1", DEDUP_TTL);
    if (Boolean.FALSE.equals(isNew)) {
      log.warn("[NotificationKafkaConsumer] 이미 처리된 이벤트, 중복 스킵 - eventId: {}", message.eventId());
      return;
    }

    List<Notification> notifications = message.userIds().stream()
        .map(userId -> Notification.builder()
            .receiver(entityManager.getReference(User.class, userId))
            .title(message.title())
            .content(message.content())
            .type(message.type())
            .build())
        .toList();

    List<Notification> saved = notificationRepository.saveAll(notifications);

    eventPublisher.publishEvent(new NotificationsPersistedEvent(saved));
  }
}
