package com.momogo.ai.problem.dto;

import java.util.List;

/**
 * LLM이 생성한 문제 묶음
 *
 * List<GeneratedProblemDto>를 직접 entity()로 받을 수도 있지만
 * record로 한 번 감싸면 JSON Schema가 더 명확해지고
 * LLM이 "items"라는 명확한 키 아래에 배열을 만들도록 유도되어 안정성이 올라간다.
 */
public record GeneratedProblemSetDto(

    List<GeneratedProblemDto> items
) {
}
