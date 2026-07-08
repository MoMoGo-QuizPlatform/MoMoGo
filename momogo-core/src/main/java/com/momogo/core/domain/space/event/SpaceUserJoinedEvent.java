package com.momogo.core.domain.space.event;

import java.util.UUID;

/**
 * 공간에 새로운 유저가 가입한 이벤트
 * @param adminUserId 알림을 받을 공간 관리자(ADMIN) ID
 * @param spaceId 공간 ID
 * @param joinedUserId 새로 가입한 유저 ID
 */
public record SpaceUserJoinedEvent(
    UUID adminUserId,
    UUID spaceId,
    UUID joinedUserId
) {
}
