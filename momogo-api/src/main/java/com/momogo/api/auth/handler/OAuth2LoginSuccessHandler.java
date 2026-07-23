package com.momogo.api.auth.handler;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.dto.JwtInformation;
import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.core.domain.user.dto.response.UserResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Value("${app.oauth2.success-redirect-url}")
    private String redirectUrl;

    /**
     * 소셜(구글/카카오) 로그인 인증 성공 시 JWT 토큰 발행 및 리다이렉션
     * 소셜 인증이 최종 성공하여 시큐리티 컨텍스트에 인증 정보가 등록되면 호출되며, 다음 로직을 처리합니다
     * 1. 인증 객체 파싱: 인증 객체(Principal)로부터 커스텀 회원 상세 정보(MoMoGoUserDetails)를 추출합니다.
     * 2. JWT 토큰 생성: 유저 정보를 기반으로 Access Token과 Refresh Token을 발급합니다.
     * 3. 레지스트리 등록: 발급된 토큰 세트를 JwtRegistry에 등록하여 무효화 및 관리를 수행합니다.
     * 4. 보안 쿠키 설정: 탈취 위험이 높은 Refresh Token은 클라이언트 자바스크립트가 접근할 수 없도록
     *    HttpOnly, Secure 속성이 적용된 보안 쿠키에 탑재합니다.
     * 5. 리다이렉트: 최종 성공 URL(app.oauth2.success-redirect-url) 뒤에 쿼리 파라미터(token=...)로 Access Token을 실어 리다이렉트(sendRedirect) 시킴으로써,
     *    프론트엔드가 토큰을 즉시 획득할 수 있도록 합니다.
     *
     * @param request HttpServletRequest 요청 객체
     * @param response HttpServletResponse 응답 객체
     * @param authentication 시큐리티 인증 객체 (OAuth2UserDetailsService에서 리턴된 MoMoGoUserDetails 포함)
     * @throws IOException 입출력 예외 발생 시
     * @throws ServletException 서블릿 예외 발생 시
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof MoMoGoUserDetails userDetails) {
            try {
                UserResponse userResponse = userDetails.getUserResponse();

                // 새 Access/Refresh JWT 토큰 발급
                String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

                // JwtRegistry 등록 (무효화 및 세션 등록)
                JwtInformation jwtInformation = new JwtInformation(userResponse, accessToken, refreshToken);
                jwtRegistry.registerJwtInformation(jwtInformation);

                // 리프레시 토큰 보안 쿠키 추가(HttpOnly)
                jwtTokenProvider.addRefreshCookie(response, refreshToken);

                log.info("[OAuth2LoginSuccessHandler] 소셜 로그인 성공 - 쿠키 발급 완료");
                response.sendRedirect(redirectUrl);

            } catch (Exception e) {
                log.error("[OAuth2LoginSuccessHandler] 토큰 생성 및 리다이렉션 실패", e);
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "인증 처리 중 서버 내부 오류가 발생했습니다."
                );
            }
        } else {
            log.error("[OAuth2LoginSuccessHandler] 객체 타입이 일치하지 않습니다. 실제 타입: {}", 
                    authentication.getPrincipal().getClass().getName());
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "유효하지 않은 인증 정보입니다."
            );
        }
    }
}
