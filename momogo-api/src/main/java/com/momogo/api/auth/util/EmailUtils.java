package com.momogo.api.auth.util;

public final class EmailUtils {

    private EmailUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 이메일 주소를 마스킹 처리합니다.
     * 예: test@example.com -> te***@example.com
     *
     * @param email 마스킹할 이메일 주소
     * @return 마스킹된 이메일 주소
     */
    public static String maskEmail(String email) {
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
}
