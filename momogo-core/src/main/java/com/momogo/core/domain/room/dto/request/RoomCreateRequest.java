package com.momogo.core.domain.room.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 평가 시험 방 생성 요청 DTO
 * @param name 시험 이름
 * @param description 시험 설명
 * @param testStartAt 시험 시작 시간
 * @param testEndAt 시험 종료 시간
 * @param userIds 시험 응시 대상자
 * @param problems 시험 문제
 */
public record RoomCreateRequest(
    @NotBlank(message = "시험 이름은 필수입니다.")
    @Size(max = 50, message = "시험 이름은 50자 이내여야 합니다.")
    String name,
    @Size(max = 500, message = "시험 설명은 500자 이내여야 합니다.")
    String description,
    @NotNull(message = "시험 시작 시간은 필수입니다.")
    @FutureOrPresent(message = "시험 시작 시간은 현재 시간 이후여야 합니다.")
    OffsetDateTime testStartAt,
    @NotNull(message = "시험 종료 시간은 필수입니다.")
    @Future(message = "시험 종료 시간은 현재 시간 이후여야 합니다.")
    OffsetDateTime testEndAt,
    @NotEmpty(message = "시험 응시 대상자는 최소 1명 이상 지정해야 합니다.")
    List<UUID> userIds,
    @NotEmpty(message = "시험 문제는 최소 1개 이상 등록해야 합니다.")
    List<RoomProblemCreatedRequest> problems
) {

}
