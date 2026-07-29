package com.momogo.ai.problem.dto;

import java.util.List;

/**
 * AI 채점 결과 리스트 래퍼 DTO
 * @param items 채점 결과 리스트
 */
public record AiGradingResultSetDto(
    List<AiGradingResultDto> items
) {

}
