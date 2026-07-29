package com.momogo.ai.problem.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * LLM 응답 전용 DTO
 * LLM이 실제로 반환하는 데이터가 담기는 부분이라 구조가 달라져도
 * core 모듈에 영향 주지 않기 위해 ai 모듈에 추가
 *
 * @JsonPropertyOrder — LLM이 응답을 생성하는 순서를 강제한다.
 * 사람 입장에서도 문제부터 만들고, 정답 정하는 순서가 자연스럽기에 일관성을 위해 사용
 */
@JsonPropertyOrder({"name", "content", "correctAnswer", "explanation"})
public record GeneratedProblemDto(

    String name,

    String content,

    String correctAnswer,

    String explanation
) {
}
