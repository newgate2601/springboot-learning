package com.example.learning.redis.redisson;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedissonLockService {
    private final RedissonClient redissonClient;

    public RedissonLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryLock(String resource, Integer threadId) {
        RLock lock = redissonClient.getLock(resource);
        try {
            log.error("Thread {} get lock !!!" , threadId);
            return lock.tryLock(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Thread {} timeout get lock !!!", threadId);
            return false;
        }
    }

    public void unlock(String resource, Integer threadId) {
        RLock lock = redissonClient.getLock(resource);
        if (lock.isHeldByCurrentThread()) {
            log.error("Thread {} release lock !!!" , threadId);
            lock.unlock();
        }
    }
}
