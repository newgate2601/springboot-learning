package com.example.learning.security_config;

import com.example.learning.repository.client.ClientJpaRepository;
import com.example.learning.repository.client.RegisterClientRepository;
import com.example.learning.security_config.passwordgranttype.PasswordAuthenticationConverter;
import com.example.learning.security_config.passwordgranttype.PasswordAuthenticationProvider;
import com.example.learning.service.UserService;
import com.example.learning.token_config.CustomPayloadValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

// https://www.appsdeveloperblog.com/spring-authorization-server-tutorial/
// config model: https://docs.spring.io/spring-authorization-server/reference/1.4/configuration-model.html
// iss: https://datatracker.ietf.org/doc/html/rfc8414#section-2
// OAuth2 Client authentication: https://datatracker.ietf.org/doc/html/rfc6749#section-2.3

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class OAuth2Config {
    private final ClientJpaRepository clientJpaRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private static final AuthorizationGrantType PASSWORD_GRANT_TYPE = new AuthorizationGrantType("custom_password");
    private static final AuthorizationGrantType CLIENT_CREDENTIALS_GRANT_TYPE = new AuthorizationGrantType("client_credentials");
    private final JdbcTemplate jdbcTemplate;
//    private final JpaOAuth2AuthorizationService jdbcOAuth2AuthorizationService;

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
                .authorizationServerSettings(authorizationServerSettings())
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenRequestConverter(
                                        PasswordAuthenticationConverter.builder()
                                                .build()
                                )
                                .authenticationProvider(
                                        PasswordAuthenticationProvider.builder()
//                                                .authorizationService(authorizationService())
                                                .authorizationService(authorizationService())
                                                .userService(userService)
                                                .tokenGenerator(tokenGenerator())
                                                .passwordEncoder(passwordEncoder)
                                                .build()
                                )
                );
//                .clientAuthentication(clientAuthentication -> {
//                    clientAuthentication
//                            .authenticationConverter(authenticationConverter());
////                            .authenticationConverters(authenticationConvertersConsumer)
////                            .authenticationProvider(authenticationProvider());
////                            .authenticationProviders(authenticationProvidersConsumer)
////                            .authenticationSuccessHandler(authenticationSuccessHandler)
////                            .errorResponseHandler(errorResponseHandler);
//                });
        return http.build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository());
    }

    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(accessTokenCustomizer());

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator()
        );
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> accessTokenCustomizer() {
        return context -> {
            OAuth2ClientAuthenticationToken principal = context.getPrincipal();
            if (PASSWORD_GRANT_TYPE.equals(context.getAuthorizationGrantType())) {
                context.getClaims().claims(claims -> {
                    claims.entrySet().removeIf(entry -> {
                        String claimKey = entry.getKey();
                        return !claimKey.equals("jti") && !claimKey.equals("exp");
                    });
                });
                CustomPayloadValue customPayloadValue = (CustomPayloadValue) principal.getDetails();
                if (Objects.nonNull(customPayloadValue.getAti())){
                    context.getClaims().claim("ati", customPayloadValue.getAti());
                }
                else {
                    Map<String, Object> claimsMap = context.getClaims().build().getClaims();
                    customPayloadValue.setAti((String) claimsMap.get("jti"));
                }
                context.getClaims().claim("exp", 9999);
                context.getClaims().claim("user_id", customPayloadValue.getUserId());
                context.getClaims().claim("user_name", customPayloadValue.getUserName());
                context.getClaims().claim("scope", customPayloadValue.getScope());
                context.getClaims().claim("authorities", customPayloadValue.getAuthorities());
                context.getClaims().claim("client_id", customPayloadValue.getClientId());
            } else if (CLIENT_CREDENTIALS_GRANT_TYPE.equals(context.getAuthorizationGrantType())) {
                context.getClaims().claim("client_test", 9999);
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    // config uri protocol endpoint + iss
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }

    // register client in db
    private RegisteredClientRepository registeredClientRepository() {
        return new RegisterClientRepository(clientJpaRepository);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }


}
//curl --location 'http://localhost:8080/oauth2/token' \
//        --header 'Authorization: Basic Y2xpZW50MTpteUNsaWVudFNlY3JldFZhbHVl' \
//        --header 'Content-Type: application/x-www-form-urlencoded' \
//        --data-urlencode 'grant_type=client_credentials'

////{
////        "access_token": "eyJraWQiOiI1OTM2YmJiNi01M2JhLTQ4NDUtYjllYS05ZWRlOGIzY2FmMDciLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjbGllbnQxIiwiYXVkIjoiY2xpZW50MSIsIm5iZiI6MTcyNjUxNzg3MiwiaXNzIjoiaHR0cDovL2F1dGgtc2VydmVyOjgwMDAiLCJleHAiOjE3MjY1MTgxNzIsImlhdCI6MTcyNjUxNzg3MiwianRpIjoiZDc1NDkzNDEtNDAyYS00OTU2LTg4NzQtZGFjYTM1OTYzMmE0In0.fpfLs_jfF5VFarYeFEf9nwugFaNvNktkSdPJlBp_GYb8O6cdNR88MYBnZ438SqBQlxbIPdFvkv52vFAxyMbqaIIaapmVkEqAEsodC9Zlf-GLZpPduhLVneNzyY3eJydFtoDUMmnk9cCa4obruDi2UC8RUC05-8_XeJ9bl01Tof7llM1bfWftFH4ztaYkopguRrir3a0kIMD_9Xh7JJ7sZqGhYIC-gQ6IisZmCJKnUPx1yst7YRQmUw_MrAYPciMowfAyW-688dbIx-r4ZvYtbf4C2RnuhZpLPftTA4Am1u4ZZ1fClBu_WDjFbR8PfOMjIqAfwijMAUSTRcagBBVArA",
////        "token_type": "Bearer",
////        "expires_in": 300
////        }

//    public AuthenticationConverter authenticationConverter() {
//        return new AuthenticationConverter() {
//            @Override
//            public Authentication convert(HttpServletRequest request) {
//                String authorizationHeader = request.getHeader("kaka");
//                if (Objects.isNull(authorizationHeader)) {
//                    System.out.println("NULL VL");
//                    throw new RuntimeException("null roi`");
//                    // Xử lý header Authorization
//                }
//                System.out.println("KO NULL");
//                return null;
//            }
//        };
//    }

//    public AuthenticationProvider authenticationProvider(){
//        return new OAuth2ClientCredentialsAuthenticationProvider(new InMemoryOAuth2AuthorizationService(),
//                new OAuth2AccessTokenGenerator());
//    }

//    private RegisteredClientRepository registeredClientRepository() {
//        return new InMemoryRegisteredClientRepository(registeredClients());
//    }


//    private List<RegisteredClient> registeredClients() {
//        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
//                .clientId("client2")
//                .clientSecret("{noop}myClientSecretValue")
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .scope("scope-a")
//                .build();
//        return List.of(registeredClient);
//    }