//package com.example.learning.responsehandle;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//@Aspect
//@Component
//public class ResponseAspect {
//
//    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping)")
//    public Object aroundGetMapping(ProceedingJoinPoint joinPoint) throws Throwable {
//        Object result = joinPoint.proceed(); // Gọi method chính
//        return Map.of("status", "success", "data", result); // Custom JSON response
//    }
//}
