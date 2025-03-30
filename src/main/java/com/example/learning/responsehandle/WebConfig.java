//package com.example.learning.responsehandle;
//
//import lombok.AllArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//@AllArgsConstructor
//public class WebConfig implements WebMvcConfigurer {
//    private final Interceptor loggingInterceptor;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(loggingInterceptor)
//                .addPathPatterns("/api/**"); // Chỉ áp dụng cho API
//    }
//}
