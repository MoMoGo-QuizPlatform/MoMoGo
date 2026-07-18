package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.momogo.core.common.util.PasswordValidationConstants;

public record UserCreateRequest(

        @NotBlank(message = "이메일은 필수로 입력해야 합니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "이름은 필수로 입력해야 합니다.")
        @Size(max = 100, message = "이름은 100자 이내여야 합니다.")
        String name,

        @NotBlank(message = "비밀번호는 필수로 입력해야 합니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
        @Pattern(
                regexp = PasswordValidationConstants.PASSWORD_REGEX,
                message = PasswordValidationConstants.PASSWORD_INVALID_MESSAGE
        )
        String password
) {
}
