package com.momogo.core.domain.space.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 공간 입장 요청 Dto
 * @param spacePassword 공간 비밀번호
 */
public record SpaceJoinRequest(
    @NotBlank(message = "공간 비밀번호를 입력해주세요.")
    String spacePassword
) {

}
