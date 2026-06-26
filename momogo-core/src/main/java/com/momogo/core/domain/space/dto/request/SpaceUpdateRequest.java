package com.momogo.core.domain.space.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @param name
 * @param description
 * @param spacePassword
 * @param spaceImageUrl
 */
public record SpaceUpdateRequest(
    @NotBlank(message = "공간 이름은 필수입니다.")
    String name,
    String description,
    String spacePassword,
    String spaceImageUrl
) {

}
