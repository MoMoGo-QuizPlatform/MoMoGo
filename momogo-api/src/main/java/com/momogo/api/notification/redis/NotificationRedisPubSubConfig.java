package com.momogo.api.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/*
 * 알림 Redis Pub/Sub 채널/리스너/컨테이너 설정.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationRedisPubSubConfig {

  // 알림 SSE 중계에 사용할 Redis 채널 토픽 정의
  @Bean
  public ChannelTopic notificationTopic() {
    return new ChannelTopic("notification-sse-channel");
  }

  // Redis 메시지를 수신할 어댑터 설정. Jackson2JsonRedisSerializer로 역직렬화를 위임해
  // subscriber가 수동으로 JSON을 파싱하지 않고 NotificationSseMessage를 바로 받도록 함.
  // Jackson2JsonRedisSerializer(Class)는 내부적으로 순정 ObjectMapper를 새로 만들어서
  // JavaTimeModule이 없어 OffsetDateTime(NotificationResponse.createdAt)을 못 읽는다.
  // 앱이 이미 쓰는(JavaTimeModule 등록된) ObjectMapper 빈을 그대로 주입해 사용한다.
  @Bean
  public MessageListenerAdapter notificationListenerAdapter(
      NotificationRedisSubscriber subscriber, ObjectMapper objectMapper) {
    MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
    adapter.setSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, NotificationSseMessage.class));
    return adapter;
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
