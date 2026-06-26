package com.momogo.core.domain.problem.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 카테고리 응답 DTO
 */
public record CategoryResponse(
    UUID id,
    String name,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

}
