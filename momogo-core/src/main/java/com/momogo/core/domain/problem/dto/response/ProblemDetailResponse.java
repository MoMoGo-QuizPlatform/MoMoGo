package com.momogo.core.domain.problem.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문제 편집용 단건 조회 응답 DTO (정답 포함, ADMIN 전용)
 */
public record ProblemDetailResponse(

    UUID id,
    UUID spaceId,
    UUID categoryId,
    String categoryName,
    String name,
    String content,
    String correctAnswer,
    String explanation,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
