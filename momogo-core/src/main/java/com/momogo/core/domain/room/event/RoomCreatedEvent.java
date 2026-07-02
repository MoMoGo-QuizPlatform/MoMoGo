package com.momogo.core.domain.room.event;

import java.util.Set;
import java.util.UUID;

/**
 * 평가 시험 방 생성 이벤트
 * @param roomId 평가 시험 방 id
 * @param spaceId 공간 id
 * @param name 평가 시험 방 이름
 * @param userIds 시험 응시 대상자 (방어적 복사 적용)
 */
public record RoomCreatedEvent(
    UUID roomId,
    UUID spaceId,
    String name,
    Set<UUID> userIds
) {

  // compact 생성자를 통해 인자로 들어온 리스트를 불변 리스트로 방어적 복사
  public RoomCreatedEvent {
    userIds = Set.copyOf(userIds);
  }
}
