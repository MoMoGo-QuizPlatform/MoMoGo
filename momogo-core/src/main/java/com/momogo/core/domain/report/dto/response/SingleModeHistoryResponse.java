package com.momogo.core.domain.report.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 싱글모드 문제 풀이 이력 응답 Dto
 * @param problemId 문제 아이디
 * @param problemName 문제 이름
 * @param categoryName 문제 카테고리 이름
 * @param isSolved 정답 여부 (가장 최근 제출 기준)
 * @param userAnswer 가장 최근 제출 답안
 * @param correctAnswer 정답
 * @param solvedAt 최초 풀이 시각
 */
public record SingleModeHistoryResponse(
    UUID problemId,
    String problemName,
    String categoryName,
    boolean isSolved,
    String userAnswer,
    String correctAnswer,
    OffsetDateTime solvedAt
) {
}
