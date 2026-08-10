package com.momogo.realtime.websocket.dto.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 실시간 시험방 응시 상태 구분용 ENUM
 */
@Getter
@RequiredArgsConstructor
public enum RoomRealtimeStatus {

  ENTER("시험방 입장"),
  PROGRESS("문제 풀이 진행 중"),
  SUBMIT("답안 제출 완료"),
  LEAVE("시험방 퇴장");

  private final String description;
}
