package com.momogo.core.domain.user.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.util.EmailFormatter;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.mapper.UserMapper;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 DTO 캐시 조회 기능을 담당하는 전용 서비스 컴포넌트입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * 이메일 기반 UserResponse DTO 캐싱 조회 메서드입니다.
     * Redis 캐시에 존재하면 DB 접근 없이 즉시 반환하며, 캐시 미스 시 DB에서 조회하여 Redis에 저장합니다.
     *
     * @param email 유저 이메일
     * @return UserResponse DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user_dtos", key = "T(com.momogo.core.common.util.EmailFormatter).normalize(#email)")
    public UserResponse getUserResponseCached(String email) {
        String normalizedEmail = EmailFormatter.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[UserCacheService] 유저를 찾을 수 없습니다, email: {}", EmailFormatter.mask(normalizedEmail));
                    return new BusinessException(UserErrorCode.NOT_FOUND);
                });
        return userMapper.toResponse(user);
    }
}
