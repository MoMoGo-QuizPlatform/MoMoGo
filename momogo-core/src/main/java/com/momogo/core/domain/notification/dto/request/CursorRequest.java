package com.momogo.core.domain.notification.dto.request;

import com.momogo.core.domain.notification.exception.NotificationErrorCode;
import com.momogo.core.domain.notification.exception.NotificationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public record CursorRequest(
    String cursor,       //기본 커서
    UUID idAfter,        //보조 커서
    @Min(1) @Max(100)
    Integer limit //데이터 개수
) {

  //limit 기본값 바인딩
  public CursorRequest {
    if (limit == null) {
      limit = 20;
    }
  }

  //파싱된 커서 결과를 묶어서 반환할 값 객체
  public record ParsedCursor(UUID lastId, OffsetDateTime lastCreatedAt) {}

  public ParsedCursor parse() {
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;

    if (hasCursor != hasIdAfter) {
      throw new NotificationException(NotificationErrorCode.INVALID_CURSOR, "cursor와 idAfter는 함께 전달되어야 합니다.");
    }

    if (!hasCursor) {
      return new ParsedCursor(null, null);
    }

    try {
      return new ParsedCursor(idAfter, OffsetDateTime.parse(cursor));
    } catch (DateTimeParseException e) {
      throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
    }
  }
}
