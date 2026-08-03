package com.momogo.realtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.momogo.realtime.websocket.pubsub.RedisMessageSubscriber;

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
    return container;
  }
}
