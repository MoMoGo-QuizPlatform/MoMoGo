package com.momogo.core.domain.notification.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponse<T> (
    List<T> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount
) {

}
