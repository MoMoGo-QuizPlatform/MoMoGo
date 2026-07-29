package com.momogo.core.domain.report.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 내 평가 시험 리뷰(응시 결과 상세) 응답 Dto
 * @param roomId 평가 시험방 아이디
 * @param roomName 평가 시험방 이름
 * @param description 평가 시험방 설명
 * @param score 취득 점수
 * @param problems 문항별 채점 결과 목록
 */
public record MyExamDetailResponse(
    UUID roomId,
    String roomName,
    String description,
    int score,
    List<MyExamProblemResultResponse> problems
) {
}
