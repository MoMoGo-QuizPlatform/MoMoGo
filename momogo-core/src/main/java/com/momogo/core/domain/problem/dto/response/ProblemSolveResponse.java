package com.momogo.core.domain.problem.dto.response;

/**
 * 문제 제출 후 채점 결과 및 통계 반환 DTO
 * isSolved      — 정답 여부
 * correctAnswer — 제출 후 정답 공개
 * explanation   — 해설
 * tryCount      — 전체 시도 횟수 (업데이트 후)
 * solvedCount   — 전체 정답 횟수 (업데이트 후)
 */
public record ProblemSolveResponse(

    boolean isSolved,
    String correctAnswer,
    String explanation,
    int tryCount,
    int solvedCount
) {}
