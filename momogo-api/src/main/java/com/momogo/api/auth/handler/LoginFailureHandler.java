package com.momogo.api.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.core.common.exception.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String errorMessage = "ID/PW가 올바르지 않습니다.";
        String errorCode = "AUTHENTICATION_FAILED";
        int status = HttpServletResponse.SC_UNAUTHORIZED;

        // UserDetailsService 등에서 던진 커스텀 비즈니스 예외(BusinessException) 감지
        if (exception.getCause() instanceof BusinessException businessException) {
            errorMessage = businessException.getMessage();
            errorCode = businessException.getErrorCode().getCode();
            status = businessException.getErrorCode().getHttpStatus().value();

            // 탈퇴 대기 상태(ALREADY_IN_PROGRESS_DELETE)로 인한 로그인 실패 시 복구 쿠키 발급
            if ("ALREADY_IN_PROGRESS_DELETE".equals(businessException.getErrorCode().getErrorKey())) {
                String[] usernames = request.getParameterValues("username");
                if (usernames != null && usernames.length == 1 && !usernames[0].isBlank()) {
                    String email = usernames[0];
                    try {
                        String restoreToken = jwtTokenProvider.generateRestoreToken(email);
                        ResponseCookie cookie = ResponseCookie.from("RESTORE_TOKEN", restoreToken)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(JwtTokenProvider.RESTORE_TOKEN_EXPIRATION_SECONDS)
                                .sameSite("Lax")
                                .build();
                        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    } catch (Exception e) {
                        log.error("[LoginFailureHandler] 일반 로그인 복구 토큰 생성 실패", e);
                    }
                } else {
                    log.warn("[LoginFailureHandler] 로그인 실패 처리에 유효하지 않거나 중복된 username 파라미터가 들어왔습니다.");
                }
            }
        } else if (exception instanceof LockedException || exception instanceof DisabledException) {
            log.warn("로그인 실패(제한된 계정): {}", exception.getClass().getSimpleName());
        } else {
            log.info("로그인 실패: {}", exception.getClass().getSimpleName());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", errorMessage);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        String responseBody = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(responseBody);
    }
}
