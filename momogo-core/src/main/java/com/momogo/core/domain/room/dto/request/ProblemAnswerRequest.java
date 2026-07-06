package com.momogo.core.domain.room.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 문제 답안 요청 DTO
 * @param roomProblemId 문제 ID
 * @param userAnswer 사용자 답안
 */
public record ProblemAnswerRequest(
    @NotNull(message = "문제 ID는 필수입니다.")
    UUID roomProblemId,
    String userAnswer
) {

}
