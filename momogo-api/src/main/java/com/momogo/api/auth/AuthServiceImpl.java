package com.momogo.api.auth;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.dto.JwtDto;
import com.momogo.api.auth.dto.JwtInformation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public JwtDto refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        JwtInformation current = jwtRegistry.getJwtInformationByRefreshToken(refreshToken);
        if (current == null) {
            throw new IllegalArgumentException("Inactive refresh token");
        }

        try {
            MoMoGoUserDetails userDetails = jwtTokenProvider.parseAccessToken(refreshToken);

            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            JwtInformation next = new JwtInformation(
                    current.user(),
                    newAccessToken,
                    newRefreshToken
            );

            jwtRegistry.rotateJwtInformation(refreshToken, next);

            try {
                // 쿠키에 새 리프레시 토큰 추가
                jwtTokenProvider.addRefreshCookie(response, newRefreshToken);
            } catch (Exception e) {
                // 롤백 처리
                jwtRegistry.rollbackRotateJwtInformation(refreshToken, current, newRefreshToken);
                throw e;
            }

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
