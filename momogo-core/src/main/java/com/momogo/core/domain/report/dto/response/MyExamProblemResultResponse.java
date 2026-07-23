package com.momogo.core.domain.report.dto.response;

import java.util.UUID;

/**
 * 내 평가 시험 리뷰용 문항별 채점 결과 응답 Dto
 * @param problemId 문제 아이디
 * @param problemOrder 문항 순서
 * @param name 문제 이름
 * @param content 문제 내용
 * @param userAnswer 내가 제출한 답안
 * @param correctAnswer 정답
 * @param explanation 해설
 * @param isCorrect 정답 여부 (채점 마감 전이면 false)
 */
public record MyExamProblemResultResponse(
    UUID problemId,
    Integer problemOrder,
    String name,
    String content,
    String userAnswer,
    String correctAnswer,
    String explanation,
    boolean isCorrect
) {
}
