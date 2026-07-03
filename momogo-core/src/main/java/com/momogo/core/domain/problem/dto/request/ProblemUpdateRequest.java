package com.momogo.core.domain.problem.dto.request;

/**
 * 문제 수정 DTO
 */
public record ProblemUpdateRequest(

    String name,
    String content,
    String explanation,
    String correctAnswer
) {}
