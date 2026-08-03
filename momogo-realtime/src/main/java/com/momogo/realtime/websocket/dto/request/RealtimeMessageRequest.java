package com.momogo.realtime.websocket.dto.request;

import com.momogo.realtime.websocket.dto.type.RoomRealtimeStatus;
import java.util.UUID;

/**
 * 웹소켓 실시간 응시 상태 변경 요청 DTO
 * @param roomId 시험방 ID
 * @param userId 수험생 ID
 * @param status 응시 상태
 * @param solvedCount 푼 문제 개수
 * @param details 기타 실시간 상세 정보
 */
public record RealtimeMessageRequest(
    UUID roomId,
    UUID userId,
    RoomRealtimeStatus status,
    int solvedCount,
    Object details
) {

}
