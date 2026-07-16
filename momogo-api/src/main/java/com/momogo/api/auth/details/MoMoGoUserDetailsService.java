package com.momogo.api.auth.details;

import com.momogo.core.common.util.EmailNormalizer;
import com.momogo.core.common.util.EmailMasker;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.mapper.UserMapper;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class MoMoGoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        log.debug("[MoMoGoUserDetailsService] loadUserByUsername 호출됨, email: {}", EmailMasker.mask(username));
        return loadUserDetails(username);
    }

    /**
     * JWT 토큰 검증 시 필터에서 호출하는 메서드입니다.
     * 유저 정보를 조회합니다.
     */
    public UserDetails loadUserByUsernameForToken(String username) {
        log.debug("[MoMoGoUserDetailsService] loadUserByUsernameForToken 호출됨, email: {}", EmailMasker.mask(username));
        return loadUserDetails(username);
    }

    /**
     * 공통 사용자 정보 조회 및 UserDetails 변환 메서드입니다.
     * 임시 패스워드가 존재하는 경우 로그인에 사용되는 비밀번호를 대체합니다.
     */
    private UserDetails loadUserDetails(String username) {
        String normalizedEmail = EmailNormalizer.normalize(username);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[MoMoGoUserDetailsService] 유저를 찾을 수 없습니다, email: {}", EmailMasker.mask(normalizedEmail));
                    return new BusinessException(UserErrorCode.NOT_FOUND);
                });

        UserResponse userResponse = userMapper.toResponse(user);

        // 10분 이내 유효한 임시 비밀번호가 존재한다면 검증용 비밀번호로 임시 비밀번호 사용
        String passwordForAuth = user.getPassword();
        if (user.getTempPassword() != null && user.getTempPasswordExpiredAt() != null
                && OffsetDateTime.now().isBefore(user.getTempPasswordExpiredAt())) {
            passwordForAuth = user.getTempPassword();
        }

        return new MoMoGoUserDetails(userResponse, passwordForAuth);
    }
}
