package com.example.learning.security_config;

import com.example.learning.repository.client.ClientJpaRepository;
import com.example.learning.repository.client.RegisterClientRepository;
import com.example.learning.security_config.password_grant_type.PasswordAuthenticationConverter;
import com.example.learning.security_config.password_grant_type.PasswordAuthenticationProvider;
import com.example.learning.service.UserService;
import com.example.learning.token.CustomPayloadValue;
import com.example.learning.token.TokenEndpointResponseHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

// https://www.appsdeveloperblog.com/spring-authorization-server-tutorial/
// config model: https://docs.spring.io/spring-authorization-server/reference/1.4/configuration-model.html
// iss: https://datatracker.ietf.org/doc/html/rfc8414#section-2
// OAuth2 Client authentication: https://datatracker.ietf.org/doc/html/rfc6749#section-2.3
// consent: https://chatgpt.com/c/66f292cc-6498-8010-85f5-9546e57daa07
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class OAuth2Config {
    private final ClientJpaRepository clientJpaRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private static final AuthorizationGrantType PASSWORD_GRANT_TYPE = new AuthorizationGrantType("custom_password");
    private static final AuthorizationGrantType CLIENT_CREDENTIALS_GRANT_TYPE = new AuthorizationGrantType("client_credentials");
    private static final AuthorizationGrantType REFRESH_TOKEN_GRANT_TYPE = new AuthorizationGrantType("refresh_token");
    private final JpaOAuth2AuthorizationService jpaOAuth2AuthorizationService;

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
                                        .authorizationService(jpaOAuth2AuthorizationService)
                                        .userService(userService)
                                        .tokenGenerator(tokenGenerator())
                                        .passwordEncoder(passwordEncoder)
                                        .build()
                        )
                        .accessTokenResponseHandler(new TokenEndpointResponseHandle())
                );
        return http.build();
    }

    // config attributes of Token previous creat token
    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(customPayloadToken());

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator()
        );
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> customPayloadToken() {
        return context -> {
            OAuth2ClientAuthenticationToken principal = context.getPrincipal();
            if (PASSWORD_GRANT_TYPE.equals(context.getAuthorizationGrantType())
                    || REFRESH_TOKEN_GRANT_TYPE.equals(context.getAuthorizationGrantType())) {
                context.getClaims().claims(claims -> {
                    claims.entrySet().removeIf(entry -> {
                        String claimKey = entry.getKey();
                        return !claimKey.equals("jti")
                                && !claimKey.equals("exp")
                                && !claimKey.equals("scope")
                                && !claimKey.equals("authorities");
                    });
                });
                CustomPayloadValue customPayloadValue = (CustomPayloadValue) principal.getDetails();
                if (Objects.nonNull(customPayloadValue.getAti())) {
                    context.getClaims().claim("ati", customPayloadValue.getAti());
                } else {
                    Map<String, Object> claimsMap = context.getClaims().build().getClaims();
                    customPayloadValue.setAti((String) claimsMap.get("jti"));
                }
                Instant expirationTime = ZonedDateTime.now().plusYears(10).toInstant();
                context.getClaims().claim("exp", expirationTime);
                context.getClaims().claim("user_id", customPayloadValue.getUserId());
                context.getClaims().claim("user_name", customPayloadValue.getUserName());
//                context.getClaims().claim("scope", customPayloadValue.getScope());
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
