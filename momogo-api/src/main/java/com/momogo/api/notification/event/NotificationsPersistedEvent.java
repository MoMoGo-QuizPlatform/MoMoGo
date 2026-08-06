package com.momogo.api.notification.event;

import com.momogo.core.domain.notification.entity.Notification;
import java.util.List;

/*
 * NotificationKafkaConsumer가 알림을 저장한 직후 발행하는 내부 이벤트.
 * SSE 전송을 DB 커밋 이후로 미루기 위한 용도로만 쓰인다.
 */
public record NotificationsPersistedEvent(List<Notification> notifications) {
}
