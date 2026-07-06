package com.momogo.core.domain.room.dto.request;

/**
 * 방 문제 수정 DTO
 * @param name          문제 이름
 * @param content       문제 내용
 * @param explanation   문제 해설
 * @param correctAnswer 정답 답안
 */
public record RoomProblemUpdateRequest(
    String name,
    String content,
    String explanation,
    String correctAnswer
) {
}
