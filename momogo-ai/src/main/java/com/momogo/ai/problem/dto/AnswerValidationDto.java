package com.momogo.ai.problem.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 정답 단일성 검증 결과 DTO
 * LLM이 정답이 하나의 명확한 개념만 가리키는지 판단한 결과가 담긴다.
 *
 * @JsonPropertyOrder - reason을 먼저 받아 LLM이 판단 근거 먼저 서술 ->
 *  서술된 근거 바탕으로 singleAnswer 결론 내리도록 유도한다.
 */
@JsonPropertyOrder({"reason", "singleAnswer"})
public record AnswerValidationDto(

    String reason,

    boolean singleAnswer
) {
}
