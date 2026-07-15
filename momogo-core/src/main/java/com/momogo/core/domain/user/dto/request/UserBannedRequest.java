package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserBannedRequest(
        @NotNull(message = "정지 상태 설정 값은 필수입니다.")
        Boolean banned
) {
}
