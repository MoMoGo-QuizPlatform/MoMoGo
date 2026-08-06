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

  // 새로 연결된 emitter를 등록하고, 연결 종료/타임아웃/에러 시 자동으로 정리되도록 콜백까지 함께
  // compute()로 묶어서, remove()의 computeIfPresent와 같은 key에 대해 서로 끼어들지 못하게 함
  public void register(UUID userId, SseEmitter emitter) {
    // 콜백 등록/정리는 registry.register()가 담당
    emitter.onCompletion(() -> remove(userId, emitter));
    emitter.onTimeout(() -> remove(userId, emitter));
    emitter.onError(e -> remove(userId, emitter));

    emitters.compute(userId, (id, userEmitters) -> {
      List<SseEmitter> list = userEmitters != null ? userEmitters : new CopyOnWriteArrayList<>();
      list.add(emitter);
      return list;
    });
  }

  // 유저에게 지금 연결되어 있는 모든 emitter 조회
  public List<SseEmitter> findAllByUserId(UUID userId) {
    return emitters.getOrDefault(userId, List.of());
  }

  // 이 인스턴스에 연결된 모든 emitter 조회 (하트비트 브로드캐스트용)
  public List<SseEmitter> findAll() {
    return emitters.values().stream()
        .flatMap(List::stream)
        .toList();
  }

  // 연결이 정상종료되거나, 타임아웃되거나, 에러가 났을 때 목록에서 제거
  // 유저의 emitter가 다 없어지면 메모리 누수 방지를 위해 그 유저의 key 자체도 지움
  public void remove(UUID userId, SseEmitter emitter) {
    emitters.computeIfPresent(userId, (id, userEmitters) -> {
      userEmitters.remove(emitter);
      return userEmitters.isEmpty() ? null : userEmitters;
    });
  }
}
