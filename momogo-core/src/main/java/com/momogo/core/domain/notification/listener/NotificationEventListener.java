package com.momogo.core.domain.notification.listener;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.entity.NotificationType;
import com.momogo.core.domain.notification.mapper.NotificationMapper;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import com.momogo.core.domain.notification.sse.NotificationSseService;
import com.momogo.core.domain.room.event.RoomCreatedEvent;
import com.momogo.core.domain.space.event.SpaceUserJoinedEvent;
import com.momogo.core.domain.space.event.SpaceUserRoleChangedEvent;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * 다른 도메인에서 발행한 이벤트를 받아서 알림을 저장하고 SSE로 실시간 전송하는 리스너.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationSseService notificationSseService;

  // 평가 시험(방) 생성 -> 대상 유저 전원에게 알림
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRoomCreated(RoomCreatedEvent event) {
    for (UUID userId : event.userIds()) {
      notify(userId, "새로운 평가 시험이 생성되었습니다",
          event.name() + " 시험이 생성되었습니다.", NotificationType.NEW_EXAM);
    }
  }

  // 공간 내 권한 변경 -> 변경된 본인에게 알림
  // TODO: SpaceServiceImpl.changeUserRole()에서 아직 이 이벤트를 발행하지 않음.
  //  targetUser.changeRole(role) 다음 줄에 publishEvent(new SpaceUserRoleChangedEvent(...)) 추가 필요
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserRoleChanged(SpaceUserRoleChangedEvent event) {
    notify(event.targetUserId(), "권한이 변경되었습니다",
        "회원님의 권한이 " + event.role() + "(으)로 변경되었습니다.", NotificationType.ROLE_CHANGE);
  }

  // 공간에 새 유저 가입 -> 그 공간 ADMIN에게 알림
  // TODO: SpaceServiceImpl.joinSpace()에서 아직 이 이벤트를 발행하지 않음.
  //  UserRepository.findBySpaceIdAndRole 머지되면 admin 조회 후 publishEvent(new SpaceUserJoinedEvent(...)) 추가 필요
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserJoined(SpaceUserJoinedEvent event) {
    notify(event.adminUserId(), "새로운 유저가 가입했습니다",
        "관리 중인 공간에 새로운 유저가 가입했습니다.", NotificationType.NEW_USER_JOINED);
  }

  // AFTER_COMMIT 단계에서 예외가 퍼지면 원본 트랜잭션은 이미 커밋됐는데도 클라이언트가 에러 응답을 받고,
  // 반복 호출(handleRoomCreated 등) 중이면 이후 유저 알림까지 중단되므로, 실패해도 로그만 남기고 삼킴
  private void notify(UUID userId, String title, String content, NotificationType type) {
    try {
      User receiver = userRepository.findById(userId)
          .orElseThrow(() -> new BusinessException(UserErrorCode.NOT_FOUND));

      Notification notification = Notification.builder()
          .receiver(receiver)
          .title(title)
          .content(content)
          .type(type)
          .build();

      Notification saved = notificationRepository.save(notification);
      NotificationResponse response = notificationMapper.toResponse(saved);
      notificationSseService.publish(userId, response);
    } catch (Exception e) {
      log.error("알림 처리 실패 - userId: {}, type: {}", userId, type, e);
    }
  }
}
