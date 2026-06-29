package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(max = 100, message = "이름은 100자 이내여야 합니다.")
        String name,

        String currentPassword,

        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "비밀번호는 8~20자이며, 영문, 숫자, 특수문자를 적어도 하나씩 포함해야 합니다."
        )
        String newPassword
) {
}
