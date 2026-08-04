package com.momogo.realtime.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.momogo.realtime.websocket.redis.RedisMessageSubscriber;
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

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter listenerAdapter,
      ChannelTopic roomTopic
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    // 토픽과 리스너 어댑터를 바인딩하여 메시지 수신 활성화
    container.addMessageListener(listenerAdapter, roomTopic);

    // 커스텀 스레드 풀 적용
    Executor taskExecutor = createThreadPoolTaskExecutor();
    container.setTaskExecutor(taskExecutor);
    container.setSubscriptionExecutor(taskExecutor);

    // 예외 에러 로깅 처리
    container.setErrorHandler(e ->
        log.error("[Redis Pub/Sub Error] 비동기 메시지 수신/처리 중 오류 발생", e));

    return container;
  }

  private Executor createThreadPoolTaskExecutor() {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("redis-listener-");
    executor.initialize();
    return executor;
  }
}
