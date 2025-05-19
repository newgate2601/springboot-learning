package com.example.learning.redis.redislockregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class DistributedLockAspect {
    private final LockRegistry lockRegistry;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.example.learning.redis.redislockregistry.DistributedLock)")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Object distributedLockAround(ProceedingJoinPoint joinPoint // đại diện cho method hiện tại đang được thực thi
    ) throws Throwable {
        // MethodSignature sử dụng để truy cập metadata của method đang thực thi
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        String lockName = distributedLock.lockName();
        String[] lockParameters = distributedLock.parameters();
        int lockTime = distributedLock.lockTime();
        if (lockTime <= 0) {
            throw new RuntimeException("LockTime must be greater than zero");
        }
        // EvaluationContext, StandardEvaluationContext giúp Spring hiểu và chạy các biểu thức động,
        // đóng vai trò là "môi trường thực thi" cho các biểu thức SpEL
        // ví dụ như trong các dòng như: "hasRole('ADMIN') and #user.id == authentication.principal.id"; "#{user.name}"
        // Thì phần trong ngoặc #{...} hoặc #user.id == ... chính là biểu thức SpEL – Spring Expression Language
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < signature.getParameterNames().length; i++) {
            String parameter = signature.getParameterNames()[i];
            context.setVariable(parameter, args[i]);
        }
//        LockKey lockKey = new LockKey();
//        lockKey.setLockName(lockName);
//        lockKey.setParameters(new ArrayList<>());

        // ExpressionParser dùng để phân tích và hiểu biểu thức SpEL
        // Biểu thức SpEL giống như công thức trong Excel: "1 + 2", "user.age > 18", "items[0].name"...
        // ExpressionParser là cái "máy đọc công thức"

        ExpressionParser expressionParser = new SpelExpressionParser();
        List<LockKey> lockKeys = new ArrayList<>();

        for (String lockParameter : lockParameters) {
            Expression expression = expressionParser.parseExpression(lockParameter);
            Object lockValue = expression.getValue(context);
            if (Objects.isNull(lockValue)) {
                continue;
            }

            List<Object> lockValues = new ArrayList<>();
            if (lockValue instanceof Collection) {
                lockValues.addAll((Collection<?>) lockValue);
            } else {
                lockValues.add(lockValue);
            }

            for (Object lockVal : lockValues) {
                if (lockVal instanceof Serializable) {
                    lockKeys.add(
                            LockKey.builder()
                                    .lockName(lockName)
                                    .parameter(lockParameter)
                                    .value(lockVal)
                                    .build()
                    );
                } else {
                    throw new RuntimeException(
                            "Lock parameters should be implement serializable: " + lockParameter);
                }
            }
        }

        List<Lock> obtainedLocks = new ArrayList<>();
        for (LockKey lockKey : lockKeys) {
            Lock lock = lockRegistry.obtain(objectMapper.writeValueAsString(lockKey));
//            lock.tryLock(); // vào else ngay lập tức khi lock đang bị giữ
            if (lock.tryLock(lockTime, TimeUnit.SECONDS)) {
                log.info("Obtain lock: " + lockKey);
                obtainedLocks.add(lock);
            } else {
                log.error("Could not obtain lock for key: " + lock);
                unlock(obtainedLocks);
                throw new RuntimeException("Could not obtain lock for key: " + lock);
            }
        }
        Object returnVal = null;
        try {
            returnVal = joinPoint.proceed();
        } finally {
            unlock(obtainedLocks);
        }
        return returnVal;
    }

    private void unlock(List<Lock> obtainedLocks) {
        if (obtainedLocks.isEmpty()) {
            return;
        }
        for (Lock obtainedLock : obtainedLocks) {
            log.info("Unlock lock: " + obtainedLock);
            obtainedLock.unlock();
        }
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class LockKey implements Serializable {
        private String lockName;
        private String parameter;
        private Object value;
    }
}
