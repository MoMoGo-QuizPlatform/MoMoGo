package com.momogo.core.domain.room.dto.response;

import java.util.UUID;

/**
 * 관리자용 방 문제 응답 DTO
 * @param id 평가 시험 문제 ID
 * @param roomId 평가 시험 방 ID
 * @param categoryId 문제 카테고리 ID
 * @param categoryName 문제 카테고리 이름
 * @param problemOrder 문제 순서
 * @param name 문제 이름
 * @param content 문제 내용
 */
public record RoomProblemResponse(
    UUID id,
    UUID roomId,
    UUID categoryId,
    String categoryName,
    Integer problemOrder,
    String name,
    String content
) {

}
