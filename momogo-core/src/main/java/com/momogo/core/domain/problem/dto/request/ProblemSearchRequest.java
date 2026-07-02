package com.momogo.core.domain.problem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문제 목록 조회 DTO
 */
public record ProblemSearchRequest(

    UUID categoryId,

    String nameKeyword,

    String contentKeyword,

    OffsetDateTime cursor,

    UUID cursorId,

    @Min(1) @Max(100)
    Integer size
) {

  public ProblemSearchRequest {
    size = (size == null) ? 10 : size;
  }
}
