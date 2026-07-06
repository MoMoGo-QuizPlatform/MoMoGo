package com.momogo.api.notification.registry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class NotificationEmitterRegistry {

  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  // 새로 연결된 emitter를 해당 유저 목록에 추가
  public void register(UUID userId, SseEmitter emitter) {
    emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
  }

  // 유저에게 지금 연결되어 있는 모든 emitter 조회
  public List<SseEmitter> findAllByUserId(UUID userId) {
    return emitters.getOrDefault(userId, List.of());
  }

  // 연결이 정상종료되거나, 타임아웃되거나, 에러가 났을 때 목록에서 제거
  // 유저의 emitter가 다 없어지면 메모리 누수 방지를 위해 그 유저의 key 자체도 지움
  public void remove(UUID userId, SseEmitter emitter) {
    List<SseEmitter> userEmitters = emitters.get(userId);
    if (userEmitters == null) {
      return;
    }
    userEmitters.remove(emitter);
    if (userEmitters.isEmpty()) {
      emitters.remove(userId);
    }
  }
}
