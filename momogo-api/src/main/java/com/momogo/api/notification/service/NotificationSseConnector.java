package com.momogo.api.notification.service;

import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// SSE 연결 생성 계약 인터페이스
public interface NotificationSseConnector {

  SseEmitter connect(UUID userId);
}
