package com.momogo.core.domain.room.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 시험방 채점 검토 화면 응답 DTO
 * @param roomId 시험방 아이디
 * @param roomName 시험방 이름
 * @param isAiGradingInProgress AI 채점 진행 중 여부
 * @param answers 응시자별 문제별 답안 채점 목록
 */
public record RoomGradingResponse(
    UUID roomId,
    String roomName,
    Boolean isAiGradingInProgress,
    List<AnswerGradingItem> answers
) {

}
