package com.momogo.api.notification.consumer;

import com.momogo.api.notification.event.NotificationsPersistedEvent;
import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.event.NotificationEventMessage;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * NotificationEventListener가 발행한 알림 메시지를 받아 실제 DB 저장을 수행
 * DB 저장 실패 등 진짜 예외는 그대로 던져서 KafkaConfig의 재시도/DLT 정책이 처리하게 함
 * SSE 전송은 여기서 하지 않고, 저장 완료 이벤트만 발행해 NotificationSsePublishListener가
 * 트랜잭션 커밋 이후에 처리하도록 위임한다 (커밋 실패 시 미저장 알림이 클라이언트로 새는 것 방지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS)
  @Transactional
  public void consume(NotificationEventMessage message) {
    log.info("[NotificationKafkaConsumer] 카프카 알림 수신 - type: {}, targetCount: {}",
        message.type(), message.userIds().size());

    List<User> receivers = userRepository.findAllById(message.userIds());
    if (receivers.size() != message.userIds().size()) {
      log.error("알림 대상 유저 일부를 찾을 수 없음 - 요청: {}명, 조회: {}명",
          message.userIds().size(), receivers.size());
    }
    if (receivers.isEmpty()) {
      return;
    }

    List<Notification> notifications = receivers.stream()
        .map(receiver -> Notification.builder()
            .receiver(receiver)
            .title(message.title())
            .content(message.content())
            .type(message.type())
            .build())
        .toList();

    List<Notification> saved = notificationRepository.saveAll(notifications);

    eventPublisher.publishEvent(new NotificationsPersistedEvent(saved));
  }
}
