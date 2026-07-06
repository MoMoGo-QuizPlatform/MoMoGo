package com.momogo.api.auth.service;

import com.momogo.api.auth.dto.request.PasswordFindRequest;
import com.momogo.api.auth.dto.response.JwtDto;

public interface AuthService {

    JwtDto refresh(String refreshToken);

    void sendTemporaryPassword(PasswordFindRequest request);
}
