package com.momogo.ai.problem.dto;

import java.util.UUID;

/**
 * 개별 답안지에 대한 AI 채점 결과 DTO
 * @param answerId 답안지 ID
 * @param isCorrect 채점 결과 (정답 여부)
 * @param feedback AI의 채점 사유 피드백
 */
public record AiGradingResultDto(
    UUID answerId,
    boolean isCorrect,
    String feedback
) {

}
