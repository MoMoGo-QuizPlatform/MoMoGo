package com.momogo.core.common.util;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 이메일 주소를 정규화합니다 (공백 제거 및 소문자 변환).
     *
     * @param email 정규화할 이메일 주소
     * @return 정규화된 이메일 주소
     */
    public static String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
