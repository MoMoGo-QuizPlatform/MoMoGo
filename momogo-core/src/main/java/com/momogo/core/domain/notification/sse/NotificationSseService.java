package com.momogo.core.domain.notification.sse;

import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import java.util.UUID;

/*
 * 특정 유저에게 알림을 실시간으로 보내라는 명령만 정의하는 인터페이스.
 * 실시간 전송은 SseEmitter로 구현하는데, core 모듈은 spring-web에만 의존하고 있어
 * SseEmitter 타입을 쓸 수 없음.
 * 그래서 core는 뭘 해야 하는지(publish)만 정의하고, 실제 전송은 api 모듈의 구현체가 함
 * 의존성 역전 원칙(DIP)
 */
public interface NotificationSseService {

  // userId에 연결된 클라이언트가 있으면, 그 알림 데이터를 실시간으로 전송한다.
  void publish(UUID userId, NotificationResponse notification);
}
