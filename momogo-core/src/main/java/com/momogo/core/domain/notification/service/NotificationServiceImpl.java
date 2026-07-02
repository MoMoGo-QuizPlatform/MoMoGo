package com.momogo.core.domain.notification.service;

import com.momogo.core.domain.notification.dto.request.CursorRequest;
import com.momogo.core.domain.notification.dto.response.CursorResponse;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.entity.Notification;
import com.momogo.core.domain.notification.exception.NotificationErrorCode;
import com.momogo.core.domain.notification.exception.NotificationException;
import com.momogo.core.domain.notification.exception.NotificationNotFoundException;
import com.momogo.core.domain.notification.mapper.NotificationMapper;
import com.momogo.core.domain.notification.repository.NotificationRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private static final int DEFAULT_LIMIT = 20;

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
    int limit = normalizeLimit(request.limit());
    Pageable pageable = PageRequest.of(0, limit + 1);

    List<Notification> notifications = (request.cursor() == null || request.cursor().isBlank())
        ? notificationRepository.findByReceiverIdOrderByCreatedAtDescIdDesc(userId, pageable)
        : notificationRepository.findNextPageByReceiverId(
            userId, parseCursor(request.cursor()), requireIdAfter(request), pageable);

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

    long totalCount = notificationRepository.countByReceiverId(userId);

    return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount);
  }

  //limit 값 정규화
  private int normalizeLimit(Integer limit) {
    return limit == null ? DEFAULT_LIMIT : limit;
  }

  //커서를 offsetDateTime 객체로 파싱. 실패시 예외 발생
  private OffsetDateTime parseCursor(String cursor) {
    try {
      return OffsetDateTime.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
    }
  }

  //요청에 idAfter 파라미터가 포함되어있는지 확인 없으면 예외 발생
  private UUID requireIdAfter(CursorRequest request) {
    if (request.idAfter() == null) {
      throw new NotificationException(NotificationErrorCode.INVALID_CURSOR, "cursor와 idAfter는 함께 전달되어야 합니다.");
    }
    return request.idAfter();
  }
}
