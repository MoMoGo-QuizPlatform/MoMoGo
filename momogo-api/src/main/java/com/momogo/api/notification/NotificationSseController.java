package com.momogo.api.notification;

import com.momogo.api.notification.service.NotificationSseConnector;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class NotificationSseController {

  private final NotificationSseConnector notificationSseConnector;

  // produces = TEXT_EVENT_STREAM_VALUE ("text/event-stream")로 지정해야
  // 브라우저의 EventSource가 이 응답을 SSE 스트림으로 인식하고 계속 열어둠.
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId
  ) {
    return notificationSseConnector.connect(userId);
  }
}
