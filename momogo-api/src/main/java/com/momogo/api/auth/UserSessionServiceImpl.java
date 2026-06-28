package com.momogo.api.auth;

import com.momogo.core.domain.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final JwtRegistry jwtRegistry;

    @Override
    public void invalidateUserSessions(UUID userId) {
        jwtRegistry.invalidateJwtInformationByUserId(userId);
    }
}
