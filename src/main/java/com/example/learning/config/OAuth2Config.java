package com.example.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

// https://www.appsdeveloperblog.com/spring-authorization-server-tutorial/
@Configuration
@EnableWebSecurity
public class OAuth2Config {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated())
                .apply(authorizationServerConfigurer);

        authorizationServerConfigurer
                .registeredClientRepository(registeredClientRepository())
//                .authorizationService(authorizationService)
//                .authorizationConsentService(authorizationConsentService)
                .authorizationServerSettings(authorizationServerSettings());
//                .tokenGenerator(tokenGenerator);

        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }

    private RegisteredClientRepository registeredClientRepository(){
        return new InMemoryRegisteredClientRepository(registeredClients());
    }

    private List<RegisteredClient> registeredClients(){
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client2")
                .clientSecret("{noop}myClientSecretValue")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("scope-a")
                .build();
        return List.of(registeredClient);
    }

//    @Bean
//    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
//        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
//    }
//
//    @Bean
//    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
//        RSAKey rsaKey = generateRsa();
//        JWKSet jwkSet = new JWKSet(rsaKey);
//        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
//    }
//
//    private static RSAKey generateRsa() throws NoSuchAlgorithmException {
//        KeyPair keyPair = generateRsaKey();
//        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
//        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
//        return new RSAKey.Builder(publicKey)
//                .privateKey(privateKey)
//                .keyID(UUID.randomUUID().toString())
//                .build();
//    }
//
//    private static KeyPair generateRsaKey() throws NoSuchAlgorithmException {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//        keyPairGenerator.initialize(2048);
//        return keyPairGenerator.generateKeyPair();
//    }
}
//curl --location 'http://localhost:8080/oauth2/token' \
//        --header 'Authorization: Basic Y2xpZW50MTpteUNsaWVudFNlY3JldFZhbHVl' \
//        --header 'Content-Type: application/x-www-form-urlencoded' \
//        --data-urlencode 'grant_type=client_credentials'
//
////{
////        "access_token": "eyJraWQiOiI1OTM2YmJiNi01M2JhLTQ4NDUtYjllYS05ZWRlOGIzY2FmMDciLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjbGllbnQxIiwiYXVkIjoiY2xpZW50MSIsIm5iZiI6MTcyNjUxNzg3MiwiaXNzIjoiaHR0cDovL2F1dGgtc2VydmVyOjgwMDAiLCJleHAiOjE3MjY1MTgxNzIsImlhdCI6MTcyNjUxNzg3MiwianRpIjoiZDc1NDkzNDEtNDAyYS00OTU2LTg4NzQtZGFjYTM1OTYzMmE0In0.fpfLs_jfF5VFarYeFEf9nwugFaNvNktkSdPJlBp_GYb8O6cdNR88MYBnZ438SqBQlxbIPdFvkv52vFAxyMbqaIIaapmVkEqAEsodC9Zlf-GLZpPduhLVneNzyY3eJydFtoDUMmnk9cCa4obruDi2UC8RUC05-8_XeJ9bl01Tof7llM1bfWftFH4ztaYkopguRrir3a0kIMD_9Xh7JJ7sZqGhYIC-gQ6IisZmCJKnUPx1yst7YRQmUw_MrAYPciMowfAyW-688dbIx-r4ZvYtbf4C2RnuhZpLPftTA4Am1u4ZZ1fClBu_WDjFbR8PfOMjIqAfwijMAUSTRcagBBVArA",
////        "token_type": "Bearer",
////        "expires_in": 300
////        }
