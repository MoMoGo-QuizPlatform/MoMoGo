package com.momogo.core.domain.user.dto;

import com.momogo.core.domain.user.entity.enums.UserRole;
import org.hibernate.query.SortDirection;

import java.util.UUID;

public record UserSearchCondition(
        String nameLike,
        String emailLike,
        String spaceNameLike,
        UserRole role,
        String cursor,
        UUID idAfter,
        SortDirection sortDirection,
        String sortBy
) {
}
