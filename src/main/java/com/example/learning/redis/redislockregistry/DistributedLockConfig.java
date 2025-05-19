package com.example.learning.redis.redislockregistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
public class DistributedLockConfig {
    @Value("${distributed-lock.lock-name}")
    private String lockName;

    @Bean(destroyMethod = "destroy")
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockRegistry(redisConnectionFactory, lockName, 600000L);
    }
}