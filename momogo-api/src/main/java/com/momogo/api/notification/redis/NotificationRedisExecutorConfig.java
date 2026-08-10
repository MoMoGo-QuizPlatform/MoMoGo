package com.momogo.api.notification.redis;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/*
 * 알림 Redis Pub/Sub 전용 스레드 풀 설정.
 */
@Configuration
public class NotificationRedisExecutorConfig {

  // 개별 Pub/Sub 메시지 비동기 처리 전용 스레드 풀
  @Bean(name = "notificationRedisTaskExecutor")
  public ThreadPoolTaskExecutor notificationRedisTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("notification-redis-task-");
    // 큐+최대스레드가 모두 꽉 찼을 때 기본 정책(AbortPolicy)은 메시지를 버린다.
    // CallerRunsPolicy로 바꿔서, 꽉 찼을 땐 발행자 스레드가 대신 처리하게 해
    // 처리 속도가 느려지더라도 메시지가 유실되지 않도록 한다.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  // Redis SUBSCRIBE 커넥션 유지 전용 독립 스레드 풀
  @Bean(name = "notificationRedisSubscriptionExecutor")
  public ThreadPoolTaskExecutor notificationRedisSubscriptionExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("notification-redis-sub-");
    executor.initialize();
    return executor;
  }
}
