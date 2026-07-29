package com.momogo.core.domain.user.event;

import java.util.UUID;

public record PasswordChangedEvent(
        UUID userId
) {
}
