package com.momogo.realtime.config;

import com.momogo.realtime.websocket.redis.RedisMessageSubscriber;
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
public class RedisPubSubConfig {

  // 실시간 시험방 통신에 사용할 Redis 채널 토픽 정의
  @Bean
  public ChannelTopic roomTopic() {
    return new ChannelTopic("room-realtime-channel");
  }

  // Redis Message를 수신할 어댑터 설정 (subscriber의 handleMessage 메소드를 호출)
  @Bean
  public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "handleMessage");
  }

  /**
   * 1. 개별 Pub/Sub 메시지 비동기 처리 전용 스레드 풀 (Spring Bean으로 등록하여 Graceful Shutdown 보장)
   */
  @Bean(name = "redisTaskExecutor")
  public ThreadPoolTaskExecutor redisTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("redis-task-");
    executor.initialize();
    return executor;
  }

  /**
   * 2. Redis SUBSCRIBE 커넥션 롱폴링/유지 전용 독립 스레드 풀 (Spring Bean 등록)
   */
  @Bean(name = "redisSubscriptionExecutor")
  public ThreadPoolTaskExecutor redisSubscriptionExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("redis-sub-");
    executor.initialize();
    return executor;
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter listenerAdapter,
      ChannelTopic roomTopic,
      @Qualifier("redisTaskExecutor") Executor redisTaskExecutor,
      @Qualifier("redisSubscriptionExecutor") Executor redisSubscriptionExecutor
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter, roomTopic);

    // Spring Bean으로 생명주기가 안전하게 관리되는 두 스레드 풀 주입
    container.setTaskExecutor(redisTaskExecutor);
    container.setSubscriptionExecutor(redisSubscriptionExecutor);

    // 비동기 메시지 처리 중 예외 발생 시 에러 로깅 ErrorHandler
    container.setErrorHandler(e ->
        log.error("[Redis Pub/Sub Error] 비동기 메시지 수신/처리 중 오류 발생", e)
    );

    return container;
  }
}