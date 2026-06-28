package com.momogo.api.auth;

import com.momogo.api.auth.dto.JwtDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    JwtDto refresh(String refreshToken, HttpServletResponse response);
}
