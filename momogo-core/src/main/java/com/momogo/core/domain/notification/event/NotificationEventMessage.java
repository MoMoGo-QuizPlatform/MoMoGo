package com.momogo.core.domain.notification.event;

import com.momogo.core.domain.notification.entity.NotificationType;
import java.util.Set;
import java.util.UUID;

public record NotificationEventMessage(
    Set<UUID> userIds,   // 알림 받을 유저 목록
    String title,        // 알림 제목
    String content,      // 알림 내용
    NotificationType type
) {
}
