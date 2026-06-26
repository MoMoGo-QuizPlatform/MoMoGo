package com.momogo.core.domain.space.dto.request;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SpaceCursorRequest(
    String cursor,
    Integer size
) {

  // size 기본값 바인딩
  public SpaceCursorRequest {

    if (size == null) {
      size = 10;
    }
  }

  // 커서로부터 lastSpaceId 추출 및 파싱 검증
  public UUID lastSpaceId() {

    if (cursor == null || cursor.isBlank()) return null;
    try {
      return UUID.fromString(cursor.split("_")[0]);
    } catch (Exception e) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "잘못된 커서 형식입니다.");
    }
  }

  // 커서로부터 lastCreatedAt 추출 및 파싱 검증
  public OffsetDateTime lastCreatedAt() {

    if (cursor == null || cursor.isBlank()) return null;
    try {
      return OffsetDateTime.parse(cursor.split("_")[1]);
    } catch (Exception e) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "잘못된 커서 형식입니다.");
    }
  }
}
