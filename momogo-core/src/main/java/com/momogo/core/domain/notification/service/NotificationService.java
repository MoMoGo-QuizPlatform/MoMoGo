package com.momogo.core.domain.notification.service;

import java.util.UUID;

public interface NotificationService {

  void confirmNotification(UUID notificationId);
}