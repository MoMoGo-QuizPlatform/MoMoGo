package com.momogo.api.notification;

import com.momogo.core.domain.notification.dto.request.CursorRequest;
import com.momogo.core.domain.notification.dto.response.CursorResponse;
import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
      @PathVariable UUID notificationId,
      @AuthenticationPrincipal(expression = "userResponse.id") UUID receiverId
  ) {
    notificationService.confirmNotification(notificationId, receiverId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<CursorResponse<NotificationResponse>> getNotifications(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @Valid @ModelAttribute CursorRequest request
  ) {
    return ResponseEntity.ok(notificationService.getNotifications(userId, request));
  }
}
