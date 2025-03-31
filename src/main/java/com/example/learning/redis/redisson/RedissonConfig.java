package com.example.learning.redis.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://redis-11951.c85.us-east-1-2.ec2.redns.redis-cloud.com:11951")
                .setPassword("oOJNSLQSbgfhTYwIaq2ZO2aBSmD3zB7B")
                .setDatabase(0);
        return Redisson.create(config);
    }
}
