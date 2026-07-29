package com.momogo.core.domain.space.dto.request;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SpaceCursorRequest(
    String cursor,
    Integer size
) {

  // size 기본값 바인딩 및 캡핑(Capping) 적용
  public SpaceCursorRequest {
    if (size == null || size < 1) {
      size = 10;
    } else if (size > 100) {
      size = 100;
    }
  }

  // 💡 파싱된 결과를 묶어서 반환할 이너 레코드
  public record ParsedCursor(UUID lastSpaceId, OffsetDateTime lastCreatedAt) {}

  // 💡 단 한 번만 분해 및 검증을 수행하는 단일 헬퍼 메소드
  public ParsedCursor parse() {
    if (cursor == null || cursor.isBlank()) {
      return new ParsedCursor(null, null);
    }
    try {
      String[] parts = cursor.split("_");
      UUID lastSpaceId = UUID.fromString(parts[0]);
      OffsetDateTime lastCreatedAt = OffsetDateTime.parse(parts[1]);
      return new ParsedCursor(lastSpaceId, lastCreatedAt);
    } catch (Exception e) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "잘못된 커서 형식입니다.");
    }
  }
}
