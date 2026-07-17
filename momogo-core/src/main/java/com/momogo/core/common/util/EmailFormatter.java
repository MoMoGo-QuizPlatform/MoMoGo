package com.momogo.core.common.util;

import java.util.Locale;

public final class EmailFormatter {

    private EmailFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 이메일 주소를 마스킹 처리합니다.
     * 예: test@example.com -> te***@example.com
     *
     * @param email 마스킹할 이메일 주소
     * @return 마스킹된 이메일 주소
     */
    public static String mask(String email) {
        if (email == null) {
            return "null";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            if (email.length() <= 3) {
                return "*".repeat(email.length());
            }
            return email.substring(0, 3) + "***";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return "*".repeat(local.length()) + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }

    /**
     * 이메일 주소를 정규화합니다 (공백 제거 및 소문자 변환).
     *
     * @param email 정규화할 이메일 주소
     * @return 정규화된 이메일 주소
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
