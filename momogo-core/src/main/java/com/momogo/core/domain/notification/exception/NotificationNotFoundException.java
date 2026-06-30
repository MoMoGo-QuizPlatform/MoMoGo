package com.momogo.core.domain.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends NotificationException {

  public NotificationNotFoundException(UUID notificationId) {
    super(NotificationErrorCode.NOTIFICATION_NOT_FOUND,
        NotificationErrorCode.NOTIFICATION_NOT_FOUND.getMessage() + " 알림 ID: " + notificationId);
  }
}
