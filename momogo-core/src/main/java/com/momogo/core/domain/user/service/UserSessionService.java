package com.momogo.core.domain.user.service;

import java.util.UUID;

public interface UserSessionService {

    void invalidateUserSessions(UUID userId);
}
