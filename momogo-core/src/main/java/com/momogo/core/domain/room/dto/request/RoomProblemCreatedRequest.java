package com.momogo.core.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 평가 시험 문제 생성 요청 DTO
 * @param name 문제 제목
 * @param content 문제 내용
 * @param explanation 문제 해설
 * @param correctAnswer 정답
 * @param problemOrder 문제 순서
 */
public record RoomProblemCreatedRequest(

    @NotBlank(message = "문제 제목은 필수입니다.")
    String name,

    @NotBlank(message = "문제 내용은 필수입니다.")
    String content,

    String explanation,

    @NotBlank(message = "정답은 필수입니다.")
    String correctAnswer,

    @NotNull(message = "문제 순서는 필수입니다.")
    @Positive(message = "문제 순서는 양수여야 합니다.")
    Integer problemOrder
) {

}
