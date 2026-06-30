package com.momogo.core.domain.problem.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문제 단건 응답 DTO
 * 생성, 수정, 단건 조회 시 공통 사용
 */
public record ProblemResponse(

    UUID id,
    UUID spaceId,
    UUID categoryId,
    String categoryName,
    String name,
    String content,
    String explanation,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
