package com.momogo.core.domain.user.dto.response;

import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String email,
        String name,
        String profileImageUrl,
        UserRole role,
        SocialType social,
        Boolean isBanned,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt,
        UUID spaceId,
        String spaceName
) {
}
