package com.momogo.core.domain.space.dto.response;

import com.momogo.core.domain.space.entity.Space;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 *
 * @param id
 * @param name
 * @param description
 * @param spaceImageUrl
 * @param createdAt
 */
public record SpaceResponse(

    UUID id,
    String name,
    String description,
    String spaceImageUrl,
    OffsetDateTime createdAt
) {

  // Entity -> Record 변환 팩토리 메서드
  public static SpaceResponse from(Space space) {
    if (space == null) return null;
    return new SpaceResponse(
        space.getId(),
        space.getName(),
        space.getDescription(),
        space.getSpaceImageUrl(),
        space.getCreatedAt()
    );
  }
}
