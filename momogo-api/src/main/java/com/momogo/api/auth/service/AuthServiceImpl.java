package com.momogo.api.auth.service;

import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.dto.JwtDto;
import com.momogo.api.auth.dto.JwtInformation;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
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
    public JwtDto refresh(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        JwtInformation current = jwtRegistry.getJwtInformationByRefreshToken(refreshToken);
        if (current == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "만료되었거나 활성화되지 않은 토큰 세션입니다.");
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

            return JwtDto.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AuthService] 토큰 갱신 중 예외 발생", e);
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "토큰 갱신 중 시스템 에러가 발생했습니다.");
        }
    }
}
