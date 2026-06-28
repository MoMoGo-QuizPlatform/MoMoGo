package com.momogo.api.auth;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.dto.JwtDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public JwtDto refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        try {
            MoMoGoUserDetails userDetails = jwtTokenProvider.parseAccessToken(refreshToken);

            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            // 쿠키에 새 리프레시 토큰 추가
            jwtTokenProvider.addRefreshCookie(response, newRefreshToken);

            return JwtDto.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new IllegalArgumentException("Failed to refresh token", e);
        }
    }
}
