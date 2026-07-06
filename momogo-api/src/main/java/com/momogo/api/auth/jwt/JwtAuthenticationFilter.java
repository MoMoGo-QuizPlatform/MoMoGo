package com.momogo.api.auth.jwt;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.details.MoMoGoUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final MoMoGoUserDetailsService userDetailsService;
    private final JwtRegistry jwtRegistry;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/refresh") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/favicon.svg") || 
               path.startsWith("/assets/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && tokenProvider.validateAccessToken(token)) {
                if (jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
                    MoMoGoUserDetails userDetails = tokenProvider.parseAccessToken(token);
                    UserDetails currentUserDetails = userDetailsService.loadUserByUsernameForToken(userDetails.getUsername());

                    if (currentUserDetails.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        currentUserDetails,
                                        null,
                                        currentUserDetails.getAuthorities()
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        jwtRegistry.invalidateJwtInformationByUserId(userDetails.getUserResponse().id());
                        log.warn("[JwtFilter] 잠긴 계정 감지: userId= {}, 모든 JWT 세션 무효화", userDetails.getUserResponse().id());
                    }
                } else {
                    log.debug("[JwtFilter] 레지스트리에 존재하지 않거나 비활성화된 토큰입니다.");
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.error("[JwtFilter] JWT 인증 과정 중 예외 발생: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }
}
