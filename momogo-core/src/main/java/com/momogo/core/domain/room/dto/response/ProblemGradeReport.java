package com.momogo.core.domain.room.dto.response;

import java.util.UUID;

/**
 * 개별 문제 채점 리포트 DTO
 * @param problemId 문제 아이디
 * @param problemOrder 문제 순서
 * @param name 문제 이름
 * @param userAnswer 사용자 답안
 * @param correctAnswer 정답
 * @param isCorrect 정답 여부
 */
public record ProblemGradeReport(
    UUID problemId,
    Integer problemOrder,
    String name,
    String userAnswer,
    String correctAnswer,
    boolean isCorrect
) {

}
