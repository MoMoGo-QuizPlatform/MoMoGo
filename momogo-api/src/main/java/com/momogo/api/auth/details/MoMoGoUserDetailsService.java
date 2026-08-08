package com.momogo.api.auth.details;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.util.EmailFormatter;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.mapper.UserMapper;
import com.momogo.core.domain.user.repository.UserRepository;
import com.momogo.core.domain.user.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class MoMoGoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserCacheService userCacheService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        log.debug("[MoMoGoUserDetailsService] loadUserByUsername 호출됨, email: {}", EmailFormatter.mask(username));
        return loadUserDetails(username);
    }

    /**
     * 공통 사용자 정보 조회 및 UserDetails 변환 메서드입니다.
     * 임시 패스워드가 유효한 경우 로그인 비밀번호를 대체하며,
     * 3분 만료 시간이 지난 임시 비밀번호는 DB에서 자동으로 초기화(clear)합니다.
     */
    private UserDetails loadUserDetails(String username) {
        String normalizedEmail = EmailFormatter.normalize(username);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[MoMoGoUserDetailsService] 유저를 찾을 수 없습니다, email: {}", EmailFormatter.mask(normalizedEmail));
                    return new BusinessException(UserErrorCode.NOT_FOUND);
                });

        UserResponse userResponse = userMapper.toResponse(user);
        String passwordForAuth = user.getPassword();

        if (user.getTempPassword() != null) {
            if (user.isTemporaryPasswordActive(OffsetDateTime.now())) {
                // 3분 이내 유효한 임시 비밀번호가 존재한다면 검증용 비밀번호로 임시 비밀번호 사용
                passwordForAuth = user.getTempPassword();
            } else {
                // 3분 초과하여 만료된 경우 DB의 임시 비밀번호 및 만료 시각 명시적 초기화
                clearExpiredTempPassword(user);
            }
        }

        return new MoMoGoUserDetails(userResponse, passwordForAuth);
    }

    /**
     * JWT 토큰 검증 시 필터에서 호출하는 메서드입니다.
     * Redis 캐시에서 UserResponse를 조회하여 DB 접근 없이 UserDetails를 즉시 반환합니다.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsernameForToken(String username) {
        log.debug("[MoMoGoUserDetailsService] loadUserByUsernameForToken 호출됨, email: {}", EmailFormatter.mask(username));
        String normalizedEmail = EmailFormatter.normalize(username);

        // Redis 캐시에서 UserResponse DTO 조회 (캐시 미스 시 DB 조회 후 Redis 자동 저장)
        UserResponse userResponse = userCacheService.getUserResponseCached(normalizedEmail);

        // 캐시된 DTO 정보로 UserDetails 즉시 생성하여 리턴 (DB 쿼리 0회)
        return new MoMoGoUserDetails(userResponse, "");
    }

    /**
     * 만료된 임시 비밀번호를 DB에서 정리(초기화)합니다.
     */
    @Transactional
    public void clearExpiredTempPassword(User user) {
        user.clearTemporaryPassword();
        userRepository.save(user);
    }
}
