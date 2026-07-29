package com.momogo.core.domain.room.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 평가 시험 방 응답 DTO
 * @param id 평가 시험 방 id
 * @param spaceId 공간 id
 * @param name 평가 시험 방 이름
 * @param description 평가 시험 방 설명
 * @param isEnded 평가 시험 방 종료 여부
 * @param testStartAt 평가 시험 시작 시간
 * @param testEndAt 평가 시험 종료 시간
 * @param createdAt 평가 시험 방 생성 시간
 */
public record RoomResponse(
    UUID id,
    UUID spaceId,
    String name,
    String description,
    Boolean isEnded,
    OffsetDateTime testStartAt,
    OffsetDateTime testEndAt,
    OffsetDateTime createdAt
) {

}
