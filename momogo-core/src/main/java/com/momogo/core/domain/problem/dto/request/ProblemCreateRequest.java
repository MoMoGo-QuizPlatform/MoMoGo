package com.momogo.core.domain.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 문제 직접 생성 요청 DTO
 */
public record ProblemCreateRequest(

    @NotNull(message = "카테고리 ID는 필수입니다.")
    UUID categoryId,

    @NotBlank(message = "문제 이름은 필수입니다.")
    String name,

    @NotBlank(message = "문제 내용은 필수입니다.")
    String content,

    String explanation,

    @NotBlank(message = "정답은 필수입니다.")
    String correctAnswer
) {

}
