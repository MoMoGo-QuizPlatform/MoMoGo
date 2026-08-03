package com.momogo.realtime.websocket.dto.request;

import com.momogo.realtime.websocket.dto.type.RoomRealtimeStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "응시 상태(status)는 필수 입력값입니다.")
    RoomRealtimeStatus status,
    @Min(value = 0, message = "푼 문제 개수(solvedCount)는 0 이상이어야 합니다.")
    int solvedCount,
    Object details
) {

}
