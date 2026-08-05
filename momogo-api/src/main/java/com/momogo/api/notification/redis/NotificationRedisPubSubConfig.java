package com.momogo.api.notification.redis;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationRedisPubSubConfig {

  // 알림 SSE 중계에 사용할 Redis 채널 토픽 정의
  @Bean
  public ChannelTopic notificationTopic() {
    return new ChannelTopic("notification-sse-channel");
  }

  // Redis 메시지를 수신할 어댑터 설정 (subscriber의 handleMessage 메소드를 호출)
  @Bean
  public MessageListenerAdapter notificationListenerAdapter(NotificationRedisSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "handleMessage");
  }

  // 개별 Pub/Sub 메시지 비동기 처리 전용 스레드 풀
  @Bean(name = "notificationRedisTaskExecutor")
  public ThreadPoolTaskExecutor notificationRedisTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("notification-redis-task-");
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

  @Bean
  public RedisMessageListenerContainer notificationRedisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter notificationListenerAdapter,
      ChannelTopic notificationTopic,
      @Qualifier("notificationRedisTaskExecutor") Executor taskExecutor,
      @Qualifier("notificationRedisSubscriptionExecutor") Executor subscriptionExecutor
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(notificationListenerAdapter, notificationTopic);
    container.setTaskExecutor(taskExecutor);
    container.setSubscriptionExecutor(subscriptionExecutor);
    container.setErrorHandler(e ->
        log.error("[Notification Redis Pub/Sub Error] 비동기 메시지 수신/처리 중 오류 발생", e)
    );
    return container;
  }
}
