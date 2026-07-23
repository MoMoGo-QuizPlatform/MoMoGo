package com.momogo.core.domain.report.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 내가 응시한 평가 시험 목록 항목 응답 Dto
 * @param roomId 평가 시험방 아이디
 * @param roomName 평가 시험방 이름
 * @param description 평가 시험방 설명
 * @param score 취득 점수 (채점 마감 전이면 0)
 * @param totalProblems 총 문항 수
 * @param testStartAt 시험 시작 시각
 * @param testEndAt 시험 종료 시각
 * @param isEnded 채점 마감 여부
 */
public record MyExamListItemResponse(
    UUID roomId,
    String roomName,
    String description,
    int score,
    int totalProblems,
    OffsetDateTime testStartAt,
    OffsetDateTime testEndAt,
    boolean isEnded
) {
}
