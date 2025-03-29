//package com.example.learning.responsehandle;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//
//@Aspect
//@Component
//public class ResponseWrapperAspect {
//
//    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
//            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
//            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
//            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
//    public Object wrapResponse(ProceedingJoinPoint joinPoint) throws Throwable {
//        Object result = joinPoint.proceed(); // Gọi phương thức gốc
//
//        CustomResponse<Object> response = CustomResponse.builder()
//                .status(HttpStatus.OK.value())
//                .body(result) // return data
//                .build();
//        return ResponseEntity.ok(response);
//    }
//}
