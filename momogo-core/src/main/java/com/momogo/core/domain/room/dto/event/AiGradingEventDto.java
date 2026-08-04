package com.momogo.core.domain.room.dto.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiGradingEventDto (
    UUID eventId,
    UUID roomId,
    UUID userId,
    OffsetDateTime timestamp
) {

  public static AiGradingEventDto of(UUID roomId, UUID userId) {
    return new AiGradingEventDto(UUID.randomUUID(), roomId, userId, OffsetDateTime.now());
  }

  public static AiGradingEventDto of(UUID roomId) {
    return new AiGradingEventDto(UUID.randomUUID(), roomId, null, OffsetDateTime.now());
  }
}
