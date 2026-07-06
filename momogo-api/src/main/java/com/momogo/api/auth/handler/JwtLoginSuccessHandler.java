package com.momogo.api.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.api.auth.jwt.JwtRegistry;
import com.momogo.api.auth.jwt.JwtTokenProvider;
import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.dto.response.JwtDto;
import com.momogo.api.auth.dto.JwtInformation;
import com.momogo.core.domain.user.dto.response.UserResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // 응답 인코딩/콘텐츠 타입 설정
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Principal 유효성 확인 및 캐스팅
        if (authentication.getPrincipal() instanceof MoMoGoUserDetails customUserDetails) {
            try {
                // 사용자 DTO 구성
                UserResponse userResponse = customUserDetails.getUserResponse();

                // 1. 새 Access/Refresh 토큰 발급
                String accessToken = jwtTokenProvider.generateAccessToken(customUserDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(customUserDetails);

                // 2. JwtRegistry에 등록 (기존 토큰 무효화 및 새 토큰 세션 저장)
                JwtInformation jwtInformation = new JwtInformation(userResponse, accessToken, refreshToken);
                jwtRegistry.registerJwtInformation(jwtInformation);

                // 3. 리프레시 토큰 쿠키 설정
                jwtTokenProvider.addRefreshCookie(response, refreshToken);

                // 4. JwtDto 바디 전송 (Access Token 및 사용자 정보 응답)
                JwtDto jwtDto = new JwtDto(userResponse, accessToken);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(objectMapper.writeValueAsString(jwtDto));

            } catch (Exception e) {
                // 예외 발생 시 처리(500)
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(objectMapper.createObjectNode()
                        .put("success", false)
                        .put("message", "Token generation failed")
                        .toString());
            }
        } else {
            // 인증 실패 시 처리(401)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(objectMapper.createObjectNode()
                    .put("success", false)
                    .put("message", "Invalid principal")
                    .toString());
        }
    }
}
