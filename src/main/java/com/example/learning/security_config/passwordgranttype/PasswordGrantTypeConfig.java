//package com.example.learning.security_config.passwordgranttype;
//
//import lombok.AllArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
//import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//
//@Configuration
//@AllArgsConstructor
//public class PasswordGrantTypeConfig {
//    private final PasswordEncoder passwordEncoder;
//
//    @Bean
//    public OAuth2AuthorizationService auth2AuthorizationService(){
//        return new InMemoryOAuth2AuthorizationService();
//    }
//
//    @Bean
//    public UserDetailsService userDetailsService(){
//        UserDetails user = User.withUsername("user")
//                .password(passwordEncoder.encode("password"))
//                .build();
//        return new InMemoryUserDetailsManager(user);
//    }
//}
