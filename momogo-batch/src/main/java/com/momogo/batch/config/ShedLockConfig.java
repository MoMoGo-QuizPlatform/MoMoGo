package com.momogo.batch.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 동일한 잠금 이름의 @Scheduled 작업이 여러 인스턴스에서 동시에 실행되지 않도록 조정하는 분산 락 설정.
 * 잠금 만료 또는 Redis 장애 시 재실행될 수 있으므로 작업은 멱등해야 한다.
 * 스케줄러 메서드에 @SchedulerLock(name = "...")을 붙이면 이 락이 적용된다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "momogo-batch");
    }
}
