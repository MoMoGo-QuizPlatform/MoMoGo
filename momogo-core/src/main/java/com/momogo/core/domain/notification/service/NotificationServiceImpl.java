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
  public void confirmNotification(UUID notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new NotificationNotFoundException(notificationId));

    notification.confirm();
  }
}
