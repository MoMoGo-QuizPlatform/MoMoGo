package com.momogo.core.domain.user.event;

/**
 * 초기화 비밀번호 생성 이벤트
 * @param email 대상 이메일
 * @param tempPassword 초기화된 패스워드
 */
public record TemporaryPasswordGeneratedEvent(
        String email,
        String tempPassword
) {
}
