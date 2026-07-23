package com.momogo.core.domain.room.dto.request;

import java.util.UUID;

/**
 * 방 문제 수정 DTO
 * @param categoryId    문제 카테고리 아이디
 * @param name          문제 이름
 * @param content       문제 내용
 * @param explanation   문제 해설
 * @param correctAnswer 정답 답안
 */
public record RoomProblemUpdateRequest(
    UUID categoryId,
    String name,
    String content,
    String explanation,
    String correctAnswer
) {
}
