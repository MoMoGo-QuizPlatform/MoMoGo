package com.momogo.realtime.websocket.dto.response;

import com.momogo.realtime.websocket.dto.request.RealtimeMessageRequest;
import com.momogo.realtime.websocket.dto.type.RoomRealtimeStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 웹소켓 실시간 응시 상태 브로드캐스팅 응답 DTO
 * @param roomId 시험방 ID
 * @param userId 수험생 ID
 * @param status 응시 상태
 * @param solvedCount 푼 문제 개수
 * @param details 기타 실시간 상세 정보
 * @param timestamp 응답 시간
 */
public record RealtimeMessageResponse(
    UUID roomId,
    UUID userId,
    RoomRealtimeStatus status,
    int solvedCount,
    Object details,
    OffsetDateTime timestamp
) {

  /**
   * Request DTO로부터 Response DTO를 정적 팩토리 메서드로 생성
   * @param request
   * @return
   */
  public static RealtimeMessageResponse of(UUID roomId, UUID userId, RealtimeMessageRequest request) {
    return new RealtimeMessageResponse(
        roomId,
        userId,
        request.status(),
        request.solvedCount(),
        request.details(),
        OffsetDateTime.now()
    );
  }
}
