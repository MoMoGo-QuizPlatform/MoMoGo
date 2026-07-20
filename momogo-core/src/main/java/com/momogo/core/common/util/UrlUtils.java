package com.momogo.core.common.util;

public final class UrlUtils {

    private UrlUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 해당 URL 또는 문자열이 외부 HTTP/HTTPS URL인지 판단합니다.
     * (접두어가 붙은 "profile/https://..." 형태 포함)
     *
     * @param url 검사할 URL 또는 경로 문자열
     * @return 외부 HTTP/HTTPS URL인 경우 true
     */
    public static boolean isExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://")
                || url.contains("http://") || url.contains("https://");
    }
}
