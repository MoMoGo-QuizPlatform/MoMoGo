package com.momogo.core.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Spring Data Redis(RedisTemplate)는 Spring Boot의 Lettuce 기본 자동 구성에 100% 맡기고,
 * Redisson은 분산 락(RedissonClient) 전용으로 독립 구성합니다.
 * RedissonAutoConfiguration을 제외하여 수동 ConnectionFactory 설정 없이 StackOverflowError를 차단합니다.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfiguration.class
})
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    // 운영 서버에서 보안을 위해 Redis에 비밀번호를 걸어 Redisson이 안전하게 접근하기 위함
    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        var singleServerConfig = config.useSingleServer().setAddress(address);
        if (password != null && !password.isBlank()) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
