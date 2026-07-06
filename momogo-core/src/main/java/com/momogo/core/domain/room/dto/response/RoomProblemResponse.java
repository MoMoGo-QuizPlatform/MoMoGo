package com.momogo.core.domain.room.dto.response;

import java.util.UUID;

/**
 * 응시자용 평가 시험 문제 상세 응답 DTO (정답 및 해설 제외)
 * @param id 평가 시험 문제 ID
 * @param roomId 평가 시험 방 ID
 * @param problemOrder 문제 순서
 * @param name 문제 이름
 * @param content 문제 내용
 */
public record RoomProblemResponse(
    UUID id,
    UUID roomId,
    Integer problemOrder,
    String name,
    String content
) {

}
