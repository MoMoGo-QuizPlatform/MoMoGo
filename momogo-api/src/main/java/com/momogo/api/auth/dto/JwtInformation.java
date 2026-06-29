package com.momogo.api.auth.dto;

import com.momogo.core.domain.user.dto.response.UserResponse;
import java.util.Objects;


public record JwtInformation(
        UserResponse user,
        String accessToken,
        String refreshToken
) {
    /*
     * 전달받은 user 객체가 null인지 검증한다.
     * 만약 null인 경우 NullPointerException 예외를 발생시킨다.
     * null이 아닌 경우 객체 생성을 진행
     */
    public JwtInformation {
        Objects.requireNonNull(user, "유저는 null이 될 수 없습니다.");
    }
}
