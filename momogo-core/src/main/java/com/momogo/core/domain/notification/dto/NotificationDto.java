package com.momogo.core.domain.notification.dto;

import com.momogo.core.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    String title,
    String content,
    NotificationType type,
    boolean isConfirmed,
    LocalDateTime createdAt
) {
}
