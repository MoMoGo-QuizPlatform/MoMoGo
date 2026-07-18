package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.momogo.core.common.util.PasswordValidationConstants;

public record UserUpdateRequest(

        @Size(max = 100, message = "이름은 100자 이내여야 합니다.")
        String name,

        String currentPassword,

        @Pattern(
                regexp = PasswordValidationConstants.PASSWORD_REGEX,
                message = PasswordValidationConstants.PASSWORD_INVALID_MESSAGE
        )
        String newPassword,

        Boolean removeProfileImage
) {
}
