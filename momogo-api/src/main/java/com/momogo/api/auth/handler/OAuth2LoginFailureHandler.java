package com.momogo.api.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-url}")
    private String failureRedirectUrl;

    /**
     * 소셜(구글/카카오) 로그인 인증 실패 시 예외 처리를 담당하는 핸들러 클래스입니다.
     * 인증 과정에서 발생한 예외 메시지를 파싱하여 프론트엔드의 지정된 실패 리다이렉트 URL로 리다이렉트합니다.
     *
     * @param request   HttpServletRequest 요청 객체
     * @param response  HttpServletResponse 응답 객체
     * @param exception 인증 실패 시 발생한 예외 객체
     * @throws IOException      입출력 예외 발생 시
     * @throws ServletException 서블릿 예외 발생 시
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("[OAuth2LoginFailureHandler] 소셜 로그인 실패: {}", exception.getMessage());

        // 에러 메시지를 프론트엔드가 알아볼 수 있게 인코딩하여 리다이렉트 주소에 포함
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "소셜 로그인 인증에 실패했습니다.";
        String errorCode = null;

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            String oauthErrorCode = oAuth2Exception.getError().getErrorCode();
            if ("user_banned".equals(oauthErrorCode)) {
                errorCode = "USER-BANNED_USER";
            }
        }

        // 커스텀 예외 메시지 포맷팅
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(failureRedirectUrl)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));

        if (errorCode != null) {
            builder.queryParam("code", errorCode);
        }

        String targetUrl = builder.build().toUriString();

        // 프론트엔드 로그인 페이지로 리다이렉트 (프론트엔드는 URL 파라미터의 error 및 code를 읽어 알림 출력 가능)
        response.sendRedirect(targetUrl);
    }
}
