package com.momogo.core.domain.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record CursorRequest(
    String cursor,       //기본 커서
    UUID idAfter,        //보조 커서
    @Min(1) @Max(100)
    Integer limit //데이터 개수
) {

}
