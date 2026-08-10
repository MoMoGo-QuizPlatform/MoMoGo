package com.momogo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MoMoGo API Application Engine
 * Trigger CI/CD Multi-Instance Production Deployment
 */
@SpringBootApplication(scanBasePackages = "com.momogo")
@EntityScan(basePackages = "com.momogo.core.domain")
@EnableJpaRepositories(basePackages = "com.momogo.core.domain")
@EnableScheduling
public class MomogoApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MomogoApiApplication.class, args);
    }
}
