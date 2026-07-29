package com.momogo.core.domain.room.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 시험방 생성 마법사(2단계) 중 미리보기용 AI 문제 생성 요청 DTO.
 * 아직 방/카테고리가 확정되지 않은 상태에서 문제 초안만 생성하며, DB에는 아무것도 저장하지 않는다.
 * (카테고리는 미리보기 화면에서 문항별로 선택 후, 최종 방 생성 요청에 포함되어 그때 저장됨)
 *
 * @param referenceText 참고자료 텍스트
 * @param questionCount 지정 문항 수
 */
public record RoomProblemDraftAiRequest(

    @NotBlank(message = "참고자료는 필수입니다.")
    @Size(max = 10000, message = "참고자료는 10,000자를 초과할 수 없습니다.")
    String referenceText,

    @NotNull(message = "문항 수는 필수입니다.")
    @Min(value = 1, message = "최소 1개 이상의 문항이 필요합니다.")
    @Max(value = 10, message = "한 번에 최대 10개 문항까지 생성 가능합니다.")
    Integer questionCount
) {
}
