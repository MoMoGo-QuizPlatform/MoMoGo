package com.momogo.core.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    // 운영 서버에서 보안을 위해 Redis에 비밀번호를 걸어 Redisson이 안전하게 접근하기 위함
    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * Spring Boot의 RedisProperties를 바인딩하여 LettuceConnectionFactory를 명시적으로 등록합니다.
     * Redisson의 커넥션 팩토리 오버라이딩으로 인한 StackOverflowError를 차단하며,
     * spring.data.redis.*의 database, host, port, password 등 운영 설정을 모두 수용합니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisProperties.getHost() != null ? redisProperties.getHost() : host);
        configuration.setPort(redisProperties.getPort() != 0 ? redisProperties.getPort() : port);
        configuration.setDatabase(redisProperties.getDatabase());

        String pwd = (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank())
                ? redisProperties.getPassword()
                : password;
        if (pwd != null && !pwd.isBlank()) {
            configuration.setPassword(pwd);
        }
        return new LettuceConnectionFactory(configuration);
    }

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
