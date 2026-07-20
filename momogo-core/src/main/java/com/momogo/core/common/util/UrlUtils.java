package com.momogo.core.common.util;

import java.util.Locale;

public final class UrlUtils {

    private UrlUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 해당 URL 또는 문자열이 외부 HTTP/HTTPS URL인지 판단합니다 (대소문자 무시).
     * (접두어가 붙은 "profile/https://..." 형태 포함)
     *
     * @param url 검사할 URL 또는 경로 문자열
     * @return 외부 HTTP/HTTPS URL인 경우 true
     */
    public static boolean isExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        return lowerCaseUrl.contains("http://") || lowerCaseUrl.contains("https://");
    }
}
