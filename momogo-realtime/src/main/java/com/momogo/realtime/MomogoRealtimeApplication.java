package com.momogo.realtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.momogo")
@EntityScan(basePackages = "com.momogo.core.domain")
@EnableJpaRepositories(basePackages = "com.momogo.core.domain")
public class MomogoRealtimeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MomogoRealtimeApplication.class, args);
    }
}
