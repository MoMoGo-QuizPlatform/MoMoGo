package com.momogo.core.domain.user.dto.response;

import org.hibernate.query.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponse<T>(
        List<T> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        Long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
    public static <T> CursorResponse<T> of(
            List<T> data,
            String nextCursor,
            UUID nextIdAfter,
            boolean hasNext,
            Long totalCount,
            String sortBy,
            SortDirection sortDirection
    ) {
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
    }
}
