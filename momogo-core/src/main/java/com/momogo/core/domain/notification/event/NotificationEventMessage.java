package com.momogo.core.domain.notification.event;

import com.momogo.core.domain.notification.entity.NotificationType;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record NotificationEventMessage(
    UUID eventId,        // 카프카 재전달 시 중복 처리 방지용 식별자
    Set<UUID> userIds,   // 알림 받을 유저 목록
    String title,        // 알림 제목
    String content,      // 알림 내용
    NotificationType type
) {

  // of()를 거치지 않고 직접 생성해도 eventId 누락(Redis 중복 체크 무력화)과
  // userIds 원본 Set 변경으로부터 안전하도록 방어
  public NotificationEventMessage {
    Objects.requireNonNull(eventId, "eventId는 null일 수 없습니다");
    userIds = Set.copyOf(userIds);
  }

  // eventId를 매번 직접 채우다가 누락하는 실수를 막기 위한 생성 창구
  public static NotificationEventMessage of(
      Set<UUID> userIds, String title, String content, NotificationType type
  ) {
    return new NotificationEventMessage(UUID.randomUUID(), userIds, title, content, type);
  }
}
