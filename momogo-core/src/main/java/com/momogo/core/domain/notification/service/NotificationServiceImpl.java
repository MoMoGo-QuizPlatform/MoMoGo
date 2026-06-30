package com.momogo.core.domain.notification.service;

import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.exception.NotificationNotFoundException;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  //private final NotificationMapper notificationMapper;

  @Override
  @Transactional
  public void confirmNotification(UUID notificationId, UUID receiverId) {
    //TODO: [임시로직] 인증로직 추가되면 삭제할것
    if (receiverId == null) {
      throw new IllegalArgumentException("수신자 ID가 필요합니다.");
    }

    Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, receiverId)
        .orElseThrow(() -> new NotificationNotFoundException(notificationId));

    notification.confirm();
  }
}
