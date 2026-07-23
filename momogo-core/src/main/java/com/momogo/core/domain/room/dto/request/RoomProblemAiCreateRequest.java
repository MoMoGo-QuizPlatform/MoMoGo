package com.momogo.core.domain.room.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * AI 문제 자동 생성 요청 DTO (방 문제 생성)
 * @param categoryId        문제 카테고리 아이디
 * @param referenceText     참고자료 텍스트
 * @param questionCount     지정 문항 수
 */
public record RoomProblemAiCreateRequest(

    @NotNull(message = "카테고리는 필수입니다.")
    UUID categoryId,

    @NotBlank(message = "참고자료는 필수입니다.")
    @Size(max = 10000, message = "참고자료는 10,000자를 초과할 수 없습니다.")
    String referenceText,

    @NotNull(message = "문항 수는 필수입니다.")
    @Min(value = 1, message = "최소 1개 이상의 문항이 필요합니다.")
    @Max(value = 10, message = "한 번에 최대 10개 문항까지 생성 가능합니다.") // 토큰 비용 및 응답 효율 위해서 10개로 제한
    Integer questionCount
) {
}
