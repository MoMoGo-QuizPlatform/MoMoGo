package com.momogo.api.auth.dto;

import com.momogo.core.domain.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtInformation {

    private UserResponse user;
    private String accessToken;
    private String refreshToken;

}
