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
import com.momogo.core.common.util.EmailFormatter;
import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.domain.user.exception.UserErrorCode;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

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
        // PII(개인식별정보) 노출 방지를 위한 예외 메시지 이메일 마스킹 로그 출력
        log.error("[OAuth2LoginFailureHandler] 소셜 로그인 실패: {}", maskEmailsInMessage(exception.getMessage()));

        String errorMessage = "소셜 로그인 인증에 실패했습니다.";
        String errorCode = null;

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            String oauthErrorCode = oAuth2Exception.getError().getErrorCode();
            switch (oauthErrorCode) {
                case "user_banned" -> {
                    errorCode = UserErrorCode.BANNED_USER.getCode();
                    errorMessage = UserErrorCode.BANNED_USER.getMessage();
                }
                case "email_required" -> {
                    errorCode = AuthErrorCode.EMAIL_REQUIRED.getErrorKey();
                    errorMessage = AuthErrorCode.EMAIL_REQUIRED.getMessage();
                }
                case "restore_expired" -> {
                    errorCode = AuthErrorCode.RESTORE_EXPIRED.getErrorKey();
                    errorMessage = AuthErrorCode.RESTORE_EXPIRED.getMessage();
                }
                case "social_type_mismatch" -> {
                    errorCode = AuthErrorCode.SOCIAL_TYPE_MISMATCH.getErrorKey();
                    errorMessage = exception.getMessage() != null ? exception.getMessage() : AuthErrorCode.SOCIAL_TYPE_MISMATCH.getMessage();
                }
                default -> {
                    errorCode = AuthErrorCode.SOCIAL_LOGIN_FAILED.getErrorKey();
                    errorMessage = exception.getMessage() != null ? exception.getMessage() : AuthErrorCode.SOCIAL_LOGIN_FAILED.getMessage();
                }
            }
        } else {
            errorMessage = exception.getMessage() != null ? exception.getMessage() : AuthErrorCode.SOCIAL_LOGIN_FAILED.getMessage();
        }

        // PII(개인식별정보) 노출 방지를 위한 이메일 마스킹 처리
        errorMessage = maskEmailsInMessage(errorMessage);

        // 커스텀 예외 메시지 포맷팅
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(failureRedirectUrl)
                .queryParam("error", errorMessage);

        if (errorCode != null) {
            builder.queryParam("code", errorCode);
        }

        // URL 쿼리 파라미터의 한글 및 공백 깨짐을 방지하기 위해 encode() 추가
        String targetUrl = builder.build().encode().toUriString();

        // 프론트엔드 로그인 페이지로 리다이렉트 (프론트엔드는 URL 파라미터의 error 및 code를 읽어 알림 출력 가능)
        response.sendRedirect(targetUrl);
    }

    /**
     * 메시지 텍스트 내부의 모든 이메일 패턴을 감지하여 마스킹 처리합니다.
     */
    private String maskEmailsInMessage(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String email = matcher.group();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(EmailFormatter.mask(email)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
