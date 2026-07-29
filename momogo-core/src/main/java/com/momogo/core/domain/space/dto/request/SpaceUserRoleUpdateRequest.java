package com.momogo.core.domain.space.dto.request;

import com.momogo.core.domain.user.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * 공간 멤버 권한 변경 요청 Dto
 * @param userRole 변경할 권한
 */
public record SpaceUserRoleUpdateRequest(
    @NotNull(message = "변경할 권한은 필수입니다.")
    UserRole userRole
) {

}
