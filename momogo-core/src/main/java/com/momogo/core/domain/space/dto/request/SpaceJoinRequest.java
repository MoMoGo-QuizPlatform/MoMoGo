package com.momogo.core.domain.space.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @param spacePassword
 */
public record SpaceJoinRequest(
    @NotBlank(message = "공간 비밀번호를 입력해주세요.")
    String spacePassword
) {

}
