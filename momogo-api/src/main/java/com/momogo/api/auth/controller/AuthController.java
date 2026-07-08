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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
     * 논리 삭제 상태인 회원 계정을 30일 이내에 복구합니다.
     * 
     * 클라이언트 주소창 노출을 피하기 위해 쿠키(RESTORE_TOKEN)에 담긴 복구 토큰에서
     * 복구 대상 이메일을 안전하게 파싱하여 회원 조회 및 기존 비밀번호 검증을 거친 후 복구 처리합니다.
     * 복구에 성공하면 임시 복구 쿠키는 즉시 파기 처리됩니다.
     *
     * @param restoreToken 임시 복구용 JWT 토큰이 포함된 HttpOnly 쿠키 값
     * @param request      계정 복구 비밀번호를 담은 요청 DTO
     * @param response     복구 성공 후 쿠키 파기를 위해 사용할 HTTP 응답 객체
     * @return 응답 본문이 없는 ResponseEntity (HTTP 204 No Content)
     */
    @PostMapping("/restore")
    public ResponseEntity<Void> restoreUser(
            @CookieValue(name = "RESTORE_TOKEN", required = false) String restoreToken,
            @RequestBody @Valid UserRestoreRequest request,
            HttpServletResponse response
    ) {
        if (restoreToken == null || restoreToken.isBlank()) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "만료되었거나 올바르지 않은 복구 세션입니다.");
        }

        String email = jwtTokenProvider.getEmailFromRestoreToken(restoreToken);
        userService.restoreUser(email, request.password());

        // 복구 성공 후 임시 복구 쿠키 삭제 처리 (즉시 만료)
        ResponseCookie deleteCookie = ResponseCookie.from("RESTORE_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.noContent().build();
    }
}
