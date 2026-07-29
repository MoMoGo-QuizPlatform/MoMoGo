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

        } else if (exception instanceof LockedException) {
            errorMessage = "정지된 계정입니다. 관리자에게 문의하세요.";
            errorCode = "USER-BANNED_USER";
            status = HttpServletResponse.SC_FORBIDDEN;
            log.warn("로그인 실패(제한된 계정): {}", exception.getClass().getSimpleName());
        } else if (exception instanceof DisabledException) {
            log.warn("로그인 실패(비활성화 계정): {}", exception.getClass().getSimpleName());
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
