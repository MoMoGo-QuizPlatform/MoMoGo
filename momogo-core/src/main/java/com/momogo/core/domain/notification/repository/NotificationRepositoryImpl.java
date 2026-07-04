package com.momogo.core.domain.notification.repository;

import static com.momogo.core.domain.notification.entity.QNotification.notification;

import com.momogo.core.domain.notification.entity.Notification;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> findNotificationsByCursor(
      UUID receiverId,
      UUID lastNotificationId,
      OffsetDateTime lastCreatedAt,
      int limit
  ) {
    return queryFactory
        .selectFrom(notification)
        .where(
            notification.receiver.id.eq(receiverId),
            cursorCondition(lastNotificationId, lastCreatedAt)
        )
        .orderBy(notification.createdAt.desc(), notification.id.desc())
        .limit(limit)
        .fetch();
  }

  // 커서 필터링 동적 조건
  private BooleanExpression cursorCondition(UUID lastNotificationId, OffsetDateTime lastCreatedAt) {
    if (lastNotificationId == null || lastCreatedAt == null) {
      return null; // 첫 페이지 조회 시 조건 없음
    }
    return notification.createdAt.lt(lastCreatedAt)
        .or(notification.createdAt.eq(lastCreatedAt).and(notification.id.lt(lastNotificationId)));
  }
}
