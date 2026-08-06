package com.momogo.api.notification.listener;

import com.momogo.api.notification.event.NotificationsPersistedEvent;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.mapper.NotificationMapper;
import com.momogo.core.domain.notification.sse.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * NotificationKafkaConsumer의 DB 저장 트랜잭션이 커밋된 이후에만 SSE를 전송한다.
 * 커밋 전에 SSE부터 나가면, 커밋 실패 시 저장 안 된 알림을 클라이언트가 받는 문제가 생긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSsePublishListener {

  private final NotificationMapper notificationMapper;
  private final NotificationSseService notificationSseService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleNotificationsPersisted(NotificationsPersistedEvent event) {
    for (Notification notification : event.notifications()) {
      try {
        NotificationResponse response = notificationMapper.toResponse(notification);
        notificationSseService.publish(notification.getReceiver().getId(), response);
      } catch (Exception e) {
        log.error("SSE 실시간 전송 실패 - userId: {}", notification.getReceiver().getId(), e);
      }
    }
  }
}
