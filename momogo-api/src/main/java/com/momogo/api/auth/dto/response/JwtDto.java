package com.momogo.api.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.momogo.core.domain.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtDto {

    private UserResponse user;
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    @Builder.Default
    private String grantType = "Bearer";

    private Long accessTokenExpiresIn; // 액세스 토큰 만료 시간 (ms 또는 초 단위)

    public JwtDto(UserResponse user, String accessToken) {
        this.user = user;
        this.accessToken = accessToken;
        this.grantType = "Bearer";
    }
}
