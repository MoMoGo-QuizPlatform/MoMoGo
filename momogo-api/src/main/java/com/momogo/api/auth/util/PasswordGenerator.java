package com.momogo.api.auth.util;

import java.security.SecureRandom;

/**
 * 임시 비밀번호를 생성하는 유틸리티 클래스입니다.
 * 보안을 위해 회원가입에서 요청되는 비밀번호 요구사항에 대문자를 추가합니다.
 */
public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;

    private static final int TEMP_PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // 객체를 생성할 필요가 없기 때문에 인스턴스화 방지
    private PasswordGenerator() {}

    /**
     * 영문 대소문자, 숫자, 특수문자가 최소 1개 이상 포함된 10자리의 규칙을 준수하는 임시 비밀번호를 생성합니다.
     */
    public static String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);

        // 규칙 충족을 위해 각 영역에서 최소 1글자씩 선택
        sb.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        // 나머지 6자리는 전체 문자 조합 풀에서 무작위 선택
        for (int i = 0; i < TEMP_PASSWORD_LENGTH - 4; i++) {
            sb.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        // 순차적으로 들어가는 패턴을 재조립하여 보안을 강화
        char[] passwordArray = sb.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }
}
