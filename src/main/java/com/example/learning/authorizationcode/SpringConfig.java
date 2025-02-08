package com.example.learning.authorizationcode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SpringConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2LoginSuccessHandler successHandler) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(registry -> {
//                    registry.requestMatchers("/").permitAll();
//                    registry.anyRequest().authenticated();
                    registry.anyRequest().permitAll();
                })
                .oauth2Login(oauth2 -> oauth2.successHandler(successHandler)) // Gán handler
                .oauth2Login(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")  // Chuyển hướng về trang chủ sau khi đăng xuất
                        .invalidateHttpSession(true)  // Hủy session
                        .clearAuthentication(true)  // Xóa xác thực
                        .deleteCookies("JSESSIONID")  // Xóa cookie phiên
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Chấp nhận mọi nguồn gốc
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // Chấp nhận tất cả headers
        configuration.setAllowCredentials(true); // Cho phép gửi cookies nếu cần

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
