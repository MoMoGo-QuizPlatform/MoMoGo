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
  public ResponseEntity<Void> confirmNotification(@PathVariable UUID notificationId) {
    notificationService.confirmNotification(notificationId);
    return ResponseEntity.ok().build();
  }
}
