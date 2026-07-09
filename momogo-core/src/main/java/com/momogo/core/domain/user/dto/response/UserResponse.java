package com.momogo.core.domain.user.dto.response;

import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String profileImageUrl,
        UserRole role,
        SocialType social,
        Boolean banned,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
}
