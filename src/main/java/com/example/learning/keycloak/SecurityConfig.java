package com.example.learning.keycloak;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableAutoConfiguration
public class SecurityConfig {
    public static final String MANAGER = "manager";
    public static final String MEMBER = "member";
    private final JwtConverter jwtConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) ->
                authorize.requestMatchers(HttpMethod.GET, "/api/hello").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/manager/**").hasRole(MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/member/**").hasRole(MEMBER)
                        .requestMatchers(HttpMethod.GET, "/api/manager-and-member/**").hasAnyRole(MANAGER, MEMBER)
                        .anyRequest().authenticated());

        http.sessionManagement(sess -> sess.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS));
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }

}
