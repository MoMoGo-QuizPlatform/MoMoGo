package com.momogo.api.auth.service;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.dto.JwtInformation;
import com.momogo.api.auth.dto.request.PasswordFindRequest;
import com.momogo.api.auth.dto.response.JwtDto;
import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.api.auth.util.PasswordGenerator;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.event.TemporaryPasswordGeneratedEvent;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Jwt
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    // Event
    private final ApplicationEventPublisher eventPublisher;

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

    /**
     * 초기화된 비밀번호를 사용자에게 이메일로 발송
     * 비인증 상태에서 유효한 이메일(존재하는 이메일, 소셜 계정)인지 노출하지 않고 return
     *
     * @param request 복구할 이메일 정보 DTO
     */
    @Override
    @Transactional
    public void sendTemporaryPassword(PasswordFindRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || user.getSocial() != SocialType.NONE) {
            log.info("[AuthService] 임시 비밀번호 발급 조건 미충족 (미가입/소셜 계정)");
            return;
        }

        String tempPassword = PasswordGenerator.generateRandomPassword();
        String hashedTempPassword = passwordEncoder.encode(tempPassword);

        user.setTemporaryPassword(hashedTempPassword);

        eventPublisher.publishEvent(new TemporaryPasswordGeneratedEvent(user.getEmail(), tempPassword));
    }

}
