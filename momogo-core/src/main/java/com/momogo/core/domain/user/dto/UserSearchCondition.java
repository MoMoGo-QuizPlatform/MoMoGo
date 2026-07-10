package com.momogo.core.domain.user.dto;

import org.hibernate.query.SortDirection;

import java.util.UUID;

public record UserSearchCondition(
        String nameLike,
        String emailLike,
        String cursor,
        UUID idAfter,
        SortDirection sortDirection,
        String sortBy
) {
}
