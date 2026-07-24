package com.momogo.core.domain.room.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 관리자 수동 채점(정오 판정 수동 오버라이드) 요청 DTO
 * @param isCorrect 관리자가 수동으로 확인한 정답 여부
 */
public record ManualGradeRequest(
    @NotNull(message = "정답 여부는 필수입니다.")
    Boolean isCorrect
) {

}
