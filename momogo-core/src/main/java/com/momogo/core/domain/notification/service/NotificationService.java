package com.momogo.core.domain.notification.service;

import com.momogo.core.domain.notification.dto.request.CursorRequest;
import com.momogo.core.domain.notification.dto.response.CursorResponse;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import java.util.UUID;

public interface NotificationService {

  void confirmNotification(UUID notificationId, UUID receiverId);

  CursorResponse<NotificationResponse> getNotifications(UUID userId, CursorRequest request);
}