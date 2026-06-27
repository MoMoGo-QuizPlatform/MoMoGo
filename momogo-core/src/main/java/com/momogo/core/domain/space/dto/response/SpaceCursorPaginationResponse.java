package com.momogo.core.domain.space.dto.response;

import java.util.List;

/**
 * 공간 커서 페이징 응답 Dto
 * @param values 공간 목록
 * @param hasNext 다음 페이지 존재 여부
 * @param nextCursor 다음 조회를 위한 커서
 * @param <T>
 */
public record SpaceCursorPaginationResponse<T>(
    List<T> values,
    Boolean hasNext,
    String nextCursor
) {

}
