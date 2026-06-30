package com.momogo.api.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import java.util.function.Supplier;

/**
 * Single Page Application(SPA) 환경을 위한 CSRF 토큰 요청 핸들러입니다.
 * 스프링 시큐리티 6의 XOR CSRF 토큰 인코딩을 지원하면서,
 * JS 클라이언트가 쿠키에서 읽은 평문 토큰을 헤더로 보냈을 때도 정상 동작하도록 처리합니다.
 */
public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // XOR 인코딩을 적용하여 요청 속성에 CSRF 토큰을 저장
        this.xor.handle(request, response, csrfToken);
        
        // 지연 로딩(Lazy loading)되는 CSRF 토큰을 강제로 호출(get)하여
        // 첫 요청 시에도 브라우저 쿠키에 CSRF 토큰이 정상적으로 담겨 내려가도록 강제합니다.
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // 헤더에 CSRF 토큰명이 존재하는지 동적으로 확인
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        
        // 헤더에 값이 있다면 평문(plain) 핸들러를 사용하고, 없다면 파라미터 등의 XOR 핸들러를 사용하여 검증
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
