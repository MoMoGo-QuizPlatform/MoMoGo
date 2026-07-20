package com.momogo.core.common.config;

import com.momogo.core.common.security.PasswordEncryptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * 보안 및 세션 처리를 위한 대체(Fallback) 설정 클래스입니다.
 *
 * 이 클래스는 api 모듈과 같이 실제 보안/세션 구현체(Spring Security, Redis 등)를 가진 모듈이 아닌,
 * batch나 realtime 모듈처럼 해당 의존성이 없는 모듈이 구동될 때 
 * 의존성 주입(DI) 실패 오류를 방지하기 위해 사용됩니다.
 */
@Configuration
public class SecurityFallbackConfig {

    /**
     * 현재 컨텍스트에 등록된 PasswordEncryptor 빈이 없을 경우 기본 대체 빈을 등록합니다.
     * 암호화 기능이 실제로 필요 없는 모듈에서 예외 없이 로딩되도록 돕습니다.
     * 만약 이 메서드가 실제로 호출되면 지원하지 않는다는 예외를 던집니다.
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncryptor.class)
    public PasswordEncryptor passwordEncryptor() {
        return new PasswordEncryptor() {
            @Override
            public String encrypt(String rawPassword) {
                throw new UnsupportedOperationException("현재 컨텍스트에서 사용할 수 있는 PasswordEncryptor 빈이 존재하지 않습니다.");
            }

            @Override
            public boolean matches(String rawPassword, String encryptedPassword) {
                throw new UnsupportedOperationException("현재 컨텍스트에서 사용할 수 있는 PasswordEncryptor 빈이 존재하지 않습니다.");
            }
        };
    }
}
