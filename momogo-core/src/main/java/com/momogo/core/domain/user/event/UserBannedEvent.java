package com.momogo.core.domain.user.event;

import java.util.UUID;

/**
 * 유저 정지(벤) 발생 이벤트
 *
 * @param userId 대상 유저 식별자
 */
public record UserBannedEvent(
        UUID userId
) {
}
