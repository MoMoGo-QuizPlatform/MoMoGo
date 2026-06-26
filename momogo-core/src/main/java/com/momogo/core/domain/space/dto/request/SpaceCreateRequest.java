package com.momogo.core.domain.space.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @param name
 * @param description
 * @param spacePassword
 * @param spaceImageUrl
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
