package com.momogo.core.domain.space.event;

import com.momogo.core.domain.user.entity.enums.UserRole;
import java.util.UUID;

/**
 * 공간 내 유저 권한 변경 이벤트
 * @param targetUserId 권한이 변경된 유저 ID
 * @param spaceId 공간 ID
 * @param role 변경된 권한
 */
public record SpaceUserRoleChangedEvent(
    UUID targetUserId,
    UUID spaceId,
    UserRole role
) {
}
