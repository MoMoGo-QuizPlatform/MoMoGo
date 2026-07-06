package com.momogo.core.domain.room.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 방 문제 응답 DTO (ADMIN용, 정답 포함)
 *
 * @param id            방 문제 ID
 * @param roomId        방 ID
 * @param problemOrder  문제 순서
 * @param name          문제 이름
 * @param content       문제 내용
 * @param explanation   문제 해설
 * @param correctAnswer 정답 답안
 * @param createdAt     생성일
 * @param updatedAt     수정일
 */
public record RoomProblemResponse(
    UUID id,
    UUID roomId,
    Integer problemOrder,
    String name,
    String content,
    String explanation,
    String correctAnswer,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
