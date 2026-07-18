package com.momogo.core.domain.user.event;

import java.util.UUID;

public record UserDeletedEvent(
        UUID userId
) {
}
