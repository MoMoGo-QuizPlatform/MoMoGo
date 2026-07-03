package com.momogo.core.domain.notification.service;

import com.momogo.core.domain.notification.dto.request.CursorRequest;
import com.momogo.core.domain.notification.dto.request.CursorRequest.ParsedCursor;
import com.momogo.core.domain.notification.dto.response.CursorResponse;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.exception.NotificationNotFoundException;
import com.momogo.core.domain.notification.mapper.NotificationMapper;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  //특정 알림을 읽음 상태로 변경. 요청한 사용자가 알림의 수신자일때만 성공
  @Override
  @Transactional
  public void confirmNotification(UUID notificationId, UUID receiverId) {

    Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, receiverId)
        .orElseThrow(() -> new NotificationNotFoundException(notificationId));

    notification.confirm();
  }

  //사용자의 알림 목록을 조회 정렬은 항상 최신순으로 고정
  @Override
  public CursorResponse<NotificationResponse> getNotifications(UUID userId, CursorRequest request) {
    int limit = request.limit();
    ParsedCursor cursor = request.parse();

    List<Notification> notifications = notificationRepository.findNotificationsByCursor(
        userId, cursor.lastId(), cursor.lastCreatedAt(), limit + 1);

    boolean hasNext = notifications.size() > limit;
    List<Notification> content = hasNext ? notifications.subList(0, limit) : notifications;

    List<NotificationResponse> data = content.stream()
        .map(notificationMapper::toResponse)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext) {
      Notification last = content.get(content.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    Long totalCount = cursor.lastId() == null
        ? notificationRepository.countByReceiverId(userId)
        : null;

    return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount);
  }
}
