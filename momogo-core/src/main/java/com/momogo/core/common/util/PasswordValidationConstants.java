package com.momogo.core.common.util;

public final class PasswordValidationConstants {
    private PasswordValidationConstants() {
        // 인스턴스화 방지
    }

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$";
    public static final String PASSWORD_INVALID_MESSAGE = "비밀번호는 8~20자이며, 영문, 숫자, 특수문자(@$!%*#?&)를 적어도 하나씩 포함해야 합니다.";
}
