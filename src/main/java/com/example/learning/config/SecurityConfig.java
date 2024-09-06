//package com.example.learning.config;
//
//import lombok.AllArgsConstructor;
//import org.apache.http.Header;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.authorization.AuthorizationDecision;
//import org.springframework.security.authorization.ReactiveAuthorizationManager;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.security.web.server.authorization.AuthorizationContext;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.Objects;
//
//@EnableWebFluxSecurity // enable WebFlux support spring security
//@Configuration
//@AllArgsConstructor
//public class SecurityConfig {
//    @Bean
//    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .authorizeExchange((authorize) -> authorize
//                                .pathMatchers("/user/**", "/signup").permitAll()
//                                .pathMatchers("*/v3/api-docs/**").permitAll()
//                                .pathMatchers("/login").access(new LogInAuthorization())
//                                .anyExchange().authenticated() // các url không match sẽ accept hết
////                                .anyExchange().denyAll() // các url không match sẽ bị deny
//                );
//        return http.build();
//    }
//
//
//    public class LogInAuthorization implements ReactiveAuthorizationManager<AuthorizationContext> {
//        @Override
//        public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
//            // Lấy ServerWebExchange từ AuthorizationContext
//            ServerWebExchange exchange = context.getExchange();
//            // Lấy giá trị của header Authorization
//            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//
//            // Từ chối yêu cầu nếu không đúng điều kiện
//            if (Objects.isNull(authHeader) || authHeader.isEmpty() || !("1").equals(authHeader)) {
//                return Mono.just(new AuthorizationDecision(false));
//            }
//            return Mono.just(new AuthorizationDecision(true)); // Chấp nhận yêu cầu
//        }
//    }
//}
