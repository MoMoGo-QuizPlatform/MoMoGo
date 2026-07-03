package com.momogo.core.domain.notification.repository;

import com.momogo.core.domain.notification.entity.Notification;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {

  List<Notification> findNotificationsByCursor(
      UUID receiverId,
      UUID lastNotificationId,
      OffsetDateTime lastCreatedAt,
      int limit
  );
}
