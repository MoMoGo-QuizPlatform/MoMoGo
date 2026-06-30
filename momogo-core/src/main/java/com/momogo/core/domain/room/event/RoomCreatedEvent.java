package com.momogo.core.domain.room.event;

import com.momogo.core.domain.room.entity.Room;
import java.util.List;
import java.util.UUID;

/**
 * 평가 시험 방 생성 이벤트
 * @param room 평가 시험 방
 * @param userIds 시험 응시 대상자
 */
public record RoomCreatedEvent(
    Room room,
    List<UUID> userIds
) {

}
