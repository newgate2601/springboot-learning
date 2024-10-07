//package com.example.learning.tracing;
//
//import brave.Tracer;
//import brave.Tracing;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
////@ConditionalOnProperty(prefix = "management.tracing", name = "enabled", havingValue = "false")
//@Configuration
//public class NoopTracingConfig {
//    @Bean
//    public Tracer tracer() {
//        return Tracing.newBuilder()
//                .build()
//                .tracer();
//    }
//}
