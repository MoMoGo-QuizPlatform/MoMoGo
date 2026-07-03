package com.momogo.api.auth.controller;

import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.api.auth.dto.JwtDto;
import com.momogo.api.auth.service.AuthService;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    // Csrf Token 요청
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.noContent().build();
    }

    // refresh 토큰 요청
    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(
            @CookieValue(
                    name = JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            )
            String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "리프레시 토큰이 누락되었습니다.");
        }

        JwtDto jwtDto = authService.refresh(refreshToken);
        jwtTokenProvider.addRefreshCookie(response, jwtDto.getRefreshToken());

        return ResponseEntity.ok(jwtDto);
    }
}
