package com.momogo.core.domain.notification.listener;

import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.entity.NotificationType;
import com.momogo.core.domain.notification.event.NotificationEventMessage;
import com.momogo.core.domain.notification.event.NotificationKafkaProducer;
import com.momogo.core.domain.notification.mapper.NotificationMapper;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import com.momogo.core.domain.notification.sse.NotificationSseService;
import com.momogo.core.domain.room.event.RoomCreatedEvent;
import com.momogo.core.domain.space.event.SpaceUserJoinedEvent;
import com.momogo.core.domain.space.event.SpaceUserRoleChangedEvent;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * 다른 도메인에서 발행한 이벤트를 받아서 알림을 저장하고 SSE로 실시간 전송하는 리스너.
 *
 * AFTER_COMMIT 단계는 원본 트랜잭션이 이미 커밋을 마친 뒤(스레드에 바인딩된 트랜잭션 리소스가
 * 정리되는 시점) 실행되므로, 이 시점에서 수행하는 DB 쓰기는 REQUIRES_NEW로 완전히 새로운
 * 트랜잭션을 열어야 한다. 그렇지 않으면 save()가 예외 없이 반환되어도(엔티티에 ID까지 채워진
 * 상태로) 실제로는 어떤 트랜잭션에도 커밋되지 않아 DB에 반영되지 않는 문제가 발생한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationSseService notificationSseService;
  private final NotificationKafkaProducer notificationKafkaProducer;

  // 평가 시험(방) 생성 -> 대상 유저 전원에게 알림 (Kafka로 비동기 발행, 대량 발송에 따른 동기 DB 쓰기 병목 회피)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRoomCreated(RoomCreatedEvent event) {
    notificationKafkaProducer.send(NotificationEventMessage.of(
        event.userIds(),
        "새로운 평가 시험이 생성되었습니다",
        event.name() + " 시험이 생성되었습니다.",
        NotificationType.NEW_EXAM
    ));
  }

  // 공간 내 권한 변경 -> 변경된 본인에게 알림
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleUserRoleChanged(SpaceUserRoleChangedEvent event) {
    User receiver = userRepository.findById(event.targetUserId()).orElse(null);
    if (receiver == null) {
      log.error("알림 대상 유저를 찾을 수 없음 - userId: {}", event.targetUserId());
      return;
    }
    notify(receiver, "권한이 변경되었습니다",
        "회원님의 권한이 " + event.role() + "(으)로 변경되었습니다.", NotificationType.ROLE_CHANGE);
  }

  // 공간에 새 유저 가입 -> 그 공간 ADMIN에게 알림
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleUserJoined(SpaceUserJoinedEvent event) {
    User receiver = userRepository.findBySpaceIdAndRole(event.spaceId(), UserRole.ADMIN).orElse(null);
    if (receiver == null) {
      log.error("공간의 ADMIN을 찾을 수 없음 - spaceId: {}", event.spaceId());
      return;
    }
    notify(receiver, "새로운 유저가 가입했습니다",
        "관리 중인 공간에 새로운 유저가 가입했습니다.", NotificationType.NEW_USER_JOINED);
  }

  // AFTER_COMMIT 단계에서 예외가 퍼지면 원본 트랜잭션은 이미 커밋됐는데도 클라이언트가 에러 응답을 받고,
  // 반복 호출(handleRoomCreated 등) 중이면 이후 유저 알림까지 중단되므로, 실패해도 로그만 남기고 삼킴
  private void notify(User receiver, String title, String content, NotificationType type) {
    try {
      Notification notification = Notification.builder()
          .receiver(receiver)
          .title(title)
          .content(content)
          .type(type)
          .build();

      Notification saved = notificationRepository.save(notification);
      NotificationResponse response = notificationMapper.toResponse(saved);
      notificationSseService.publish(receiver.getId(), response);
    } catch (Exception e) {
      log.error("알림 처리 실패 - userId: {}, type: {}", receiver.getId(), type, e);
    }
  }
}
