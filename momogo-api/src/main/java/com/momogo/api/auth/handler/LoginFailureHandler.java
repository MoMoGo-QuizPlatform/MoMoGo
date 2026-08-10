package com.momogo.api.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.user.exception.UserErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

            // 서버 과부하(동시성 제한)로 인한 실패는 별도 로그 레벨/메시지로 구분
            if (businessException.getErrorCode() == AuthErrorCode.LOGIN_SERVER_BUSY) {
                log.warn("로그인 실패(서버 과부하 동시성 제한): {}", exception.getMessage());
            } else {
                log.warn("로그인 실패(비즈니스 예외): {} - {}", errorCode, errorMessage);
            }

        } else if (exception instanceof LockedException) {
            // MoMoGoUserDetails.isAccountNonLocked() == false -> 정지(밴) 계정
            errorMessage = UserErrorCode.BANNED_USER.getMessage();
            errorCode = UserErrorCode.BANNED_USER.getCode();
            status = UserErrorCode.BANNED_USER.getHttpStatus().value();
            log.warn("로그인 실패(정지된 계정): {}", exception.getClass().getSimpleName());

        } else if (exception instanceof DisabledException) {
            // MoMoGoUserDetails.isEnabled() == false -> 탈퇴(논리 삭제) 계정
            errorMessage = UserErrorCode.ALREADY_IN_PROGRESS_DELETE.getMessage();
            errorCode = UserErrorCode.ALREADY_IN_PROGRESS_DELETE.getCode();
            status = UserErrorCode.ALREADY_IN_PROGRESS_DELETE.getHttpStatus().value();
            log.warn("로그인 실패(탈퇴된 계정): {}", exception.getClass().getSimpleName());

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