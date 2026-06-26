package com.momogo.core.domain.space.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 공간 개설 요청 Dto
 * @param name 공간 이름
 * @param description 공간 설명
 * @param spacePassword 공간 비밀번호
 * @param spaceImageUrl 공간 이미지
 */
public record SpaceCreateRequest(

    @NotBlank(message = "공간 이름은 필수입니다.")
    String name,
    String description,
    @NotBlank(message = "공간 비밀번호는 필수입니다.")
    String spacePassword,
    String spaceImageUrl
) {

}
