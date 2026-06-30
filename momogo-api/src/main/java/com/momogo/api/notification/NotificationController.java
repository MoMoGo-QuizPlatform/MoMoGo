package com.momogo.api.notification;

import com.momogo.core.domain.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  @PatchMapping("/{notificationId}/confirm")
  public ResponseEntity<Void> confirmNotification(
      @PathVariable UUID notificationId) {
    // TODO: 인증 기능 머지 후, 실제 사용자 ID를 전달해야 함
    notificationService.confirmNotification(notificationId, null); //null값 변경예정
    return ResponseEntity.ok().build();
  }
}
