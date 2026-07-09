package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserRestoreRequest(

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        String password
) {
}
