package com.momogo.core.domain.problem.dto.response;

/**
 * AI 생성 서비스가 momogo-core에 돌려주는 문제 데이터
 * @param name          문제 이름
 * @param content       문제 내용
 * @param correctAnswer 문제 답안
 * @param explanation   문제 해설
 */
public record GeneratedProblemData(

    String name,

    String content,

    String correctAnswer,

    String explanation
) {
}
