package com.momogo.batch;

import com.momogo.core.domain.user.service.RestoreTokenValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.momogo")
@EntityScan(basePackages = "com.momogo.core.domain")
@EnableJpaRepositories(basePackages = "com.momogo.core.domain")
public class MomogoBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(MomogoBatchApplication.class, args);
    }

    /**
     * Batch 모듈 구동 시 core 모듈의 UserServiceImpl이 필수 주입을 원하므로
     * 더미 객체를 직접 등록하여 ConText 구동 실패를 방어합니다.
     */
    @Bean
    public RestoreTokenValidator dummyRestoreTokenValidator() {
        return token -> {
            throw new UnsupportedOperationException("Batch 모듈에서는 복구 토큰 검증을 지원하지 않습니다.");
        };
    }
}
