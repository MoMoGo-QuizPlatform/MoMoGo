package com.momogo.core.domain.space.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 공간 정보 응답 Dto
 * @param id 공간 id
 * @param name 공간 이름
 * @param description 공간 설명
 * @param spaceImageUrl 공간 이미지
 * @param createdAt 공간 생성일
 */
public record SpaceResponse(

    UUID id,
    String name,
    String description,
    String spaceImageUrl,
    OffsetDateTime createdAt
) {
}
