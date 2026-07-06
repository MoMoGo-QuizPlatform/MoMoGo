package com.momogo.api.auth.service;

import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.core.domain.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

/*
 * momogo-core에 있는 서비스는 JwtRegistry를 주입받아서 사용할 수 없으므로
 * 의존성 역전 원칙(DIP)로 core에 interface를 설정하고 api에 구현체를 구현함
 */
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final JwtRegistry jwtRegistry;

    @Override
    public void invalidateUserSessions(UUID userId) {
        jwtRegistry.invalidateJwtInformationByUserId(userId);
    }
}
