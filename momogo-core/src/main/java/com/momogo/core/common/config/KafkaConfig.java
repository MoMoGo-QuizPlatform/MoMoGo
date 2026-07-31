package com.momogo.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

/**
 * @KafkaListener에 공통으로 적용되는 기본 재시도/DLT 정책.
 * 개별 리스너가 @RetryableTopic으로 별도 정책을 지정하지 않으면 이 설정을 따른다.
 * (재시도: 1초 -> 2초 -> 4초 -> 8초, 최대 4회 시도 후 "{topic}.DLT" 토픽으로 격리)
 */
@Configuration
public class KafkaConfig {

    @Bean
    public RetryTopicConfiguration defaultRetryTopicConfiguration(KafkaTemplate<Object, Object> kafkaTemplate) {
        return RetryTopicConfigurationBuilder.newInstance()
                .exponentialBackoff(1000, 2.0, 30000)
                .maxAttempts(4)
                .autoCreateTopics(true, 3, (short) 1)
                .create(kafkaTemplate);
    }
}
