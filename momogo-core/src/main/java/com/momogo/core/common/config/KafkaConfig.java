package com.momogo.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

/**
 * @KafkaListener에 공통으로 적용되는 기본 재시도/DLT 정책.
 * 개별 리스너가 @RetryableTopic으로 별도 정책을 지정하지 않으면 이 설정을 따른다.
 * (최초 처리 포함 총 5회 시도 - 재시도 4회 간격은 1초 -> 2초 -> 4초 -> 8초 -
 * 후 실패하면 "{topic}-dlt" 토픽으로 격리)
 */
@Configuration
public class KafkaConfig {

    @Bean
    public RetryTopicConfiguration defaultRetryTopicConfiguration(KafkaTemplate<Object, Object> kafkaTemplate) {
        return RetryTopicConfigurationBuilder.newInstance()
                .exponentialBackoff(1000, 2.0, 30000)
                .maxAttempts(5)
                .autoCreateTopics(true, 3, (short) 1)
                .create(kafkaTemplate);
    }
}
