package com.example.learning.redis.redislockregistry;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    String lockName() default "";
    String[] parameters() default {};
    int lockTime() default 20;
}
