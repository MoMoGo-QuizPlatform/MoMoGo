package com.momogo.core.domain.space.repository;

import com.momogo.core.domain.space.entity.Space;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SpaceRepositoryCustom {

  List<Space> findUnjoinedSpacesByCursor(
      UUID userSpaceId,
      UUID lastSpaceId,
      OffsetDateTime lastCreatedAt,
      int limit
  );
}
