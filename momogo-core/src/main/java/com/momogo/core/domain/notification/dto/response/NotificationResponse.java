package com.momogo.core.domain.notification.dto.response;

import com.momogo.core.domain.notification.entity.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String content,
    NotificationType type,
    boolean isConfirmed,
    OffsetDateTime createdAt
) {
}
