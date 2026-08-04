package com.momogo.core.domain.notification.event;

import com.momogo.core.domain.notification.entity.NotificationType;
import java.util.Set;
import java.util.UUID;

public record NotificationEventMessage(
    UUID eventId,        // 카프카 재전달 시 중복 처리 방지용 식별자
    Set<UUID> userIds,   // 알림 받을 유저 목록
    String title,        // 알림 제목
    String content,      // 알림 내용
    NotificationType type
) {

  // eventId를 매번 직접 채우다가 누락하는 실수를 막기 위한 생성 창구
  public static NotificationEventMessage of(
      Set<UUID> userIds, String title, String content, NotificationType type
  ) {
    return new NotificationEventMessage(UUID.randomUUID(), userIds, title, content, type);
  }
}
