package com.momogo.core.domain.problem.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 문제 목록 커서 페이지네이션 응답 DTO
 */
public record ProblemCursorResponse(

    List<ProblemResponse> content,
    boolean hasNext,
    OffsetDateTime nextCursor,
    UUID nextCursorId
) {}
