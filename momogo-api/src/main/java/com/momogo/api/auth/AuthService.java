package com.momogo.api.auth;

import com.momogo.api.auth.dto.JwtDto;

public interface AuthService {
    JwtDto refresh(String refreshToken);
}
