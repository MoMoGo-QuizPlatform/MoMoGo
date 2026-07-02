package com.momogo.core.domain.problem.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 문제 제출 요청 DTO
 */
public record ProblemSolveRequest(

    @NotBlank(message = "답안은 필수입니다.")
    String userAnswer
) {}
