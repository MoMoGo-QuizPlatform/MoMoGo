package com.momogo.api.auth.controller;

import com.momogo.api.auth.dto.request.PasswordFindRequest;
import com.momogo.api.auth.dto.response.JwtDto;
import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.api.auth.service.AuthService;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.user.dto.request.UserRestoreRequest;
import com.momogo.core.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

    private final UserService userService;

    /**
     * CSRF 토큰을 조회합니다.
     *
     * @param csrfToken Spring Security가 주입한 CSRF 토큰 객체
     * @return 응답 본문이 없는 ResponseEntity (HTTP 204 No Content)
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh Token을 사용하여 Access Token을 재발급합니다.
     *
     * @param refreshToken 쿠키에서 추출한 Refresh Token
     * @param response     새로운 Refresh Token 쿠키를 주입할 HTTP 응답 객체
     * @return 갱신된 JWT 토큰 DTO
     */
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

    /**
     * 사용자의 임시 비밀번호를 발송합니다. (비밀번호 찾기)
     *
     * @param request 비밀번호 찾기 요청 DTO
     * @return 응답 본문이 없는 ResponseEntity (HTTP 204 No Content)
     */
    @PostMapping("/password-find")
    public ResponseEntity<Void> findPassword(@RequestBody @Valid PasswordFindRequest request) {
        authService.sendTemporaryPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 논리 삭제된 유저의 계정을 복구합니다. (30일 이내)
     *
     * @param request 계정 복구 요청 DTO
     * @return 응답 본문이 없는 ResponseEntity (HTTP 204 No Content)
     */
    @PostMapping("/restore")
    public ResponseEntity<Void> restoreUser(@RequestBody @Valid UserRestoreRequest request) {
        userService.restoreUser(request.email());
        return ResponseEntity.noContent().build();
    }
}
