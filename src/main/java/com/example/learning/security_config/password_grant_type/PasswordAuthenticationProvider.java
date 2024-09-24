package com.example.learning.security_config.password_grant_type;

import java.security.Principal;
import java.util.*;

import com.example.learning.dto.ClientRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.service.UserService;
import com.example.learning.token.CustomPayloadValue;
import lombok.Builder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

// GrantType(name)-Provider (main-processor)
// use to auth Authentication instance receiver from Converter
@Builder
public class PasswordAuthenticationProvider implements AuthenticationProvider {
    private static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType("custom_password");
    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

    private final OAuth2AuthorizationService authorizationService;
    private final UserService userService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PasswordAuthenticationToken passwordAuthenticationToken =
                (PasswordAuthenticationToken) authentication;
        String usernameRequest = passwordAuthenticationToken.getUsername();
        String passwordRequest = passwordAuthenticationToken.getPassword();
        // continue to validate scope of use if use have scopes
        Set<String> requestScopes = passwordAuthenticationToken.getScopes();
        OAuth2ClientAuthenticationToken clientPrincipal =
                (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        UserEntity userEntity = userService.getUser(usernameRequest);
        validateUser(userEntity, usernameRequest, passwordRequest);

        // addition info for context
        OAuth2ClientAuthenticationToken oAuth2ClientAuthenticationToken =
                (OAuth2ClientAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        CustomPayloadValue customPayloadValue = getCustomPayloadValue(userEntity, registeredClient);
        oAuth2ClientAuthenticationToken.setDetails(customPayloadValue);

        //-----------TOKEN BUILDERS----------
        OAuth2Authorization.Builder authorizationBuilder = getAuthorizationBuilder(registeredClient,
                clientPrincipal, requestScopes);

        //-----------ACCESS TOKEN----------
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = getTokenContextBuilder(registeredClient,
                clientPrincipal, requestScopes, passwordAuthenticationToken);
        OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                tokenContext.getAuthorizedScopes()
        );
        authorizationBuilder.accessToken(accessToken);

        //-----------REFRESH TOKEN----------
//        DefaultOAuth2TokenContext.Builder refreshTokenContextBuilder = getTokenContextBuilder(registeredClient, clientPrincipal, authorizedScopes, passwordAuthenticationToken);
//        OAuth2TokenContext refreshTokenContext = refreshTokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
//        OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(refreshTokenContext);
        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
            OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
            refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
            authorizationBuilder.refreshToken(refreshToken);

            refreshToken = new OAuth2RefreshToken(
                    generatedRefreshToken.getTokenValue(),
                    generatedRefreshToken.getIssuedAt(),
                    generatedRefreshToken.getExpiresAt()
            );
            authorizationBuilder.refreshToken(refreshToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        this.authorizationService.save(authorization);

        Map<String, Object> additionResponse = new HashMap<>();
        additionResponse.put("test1", "okeke");
        additionResponse.put("kaka", new ClientRequest("1", "2"));
        // config response for login
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientPrincipal,
                accessToken,
                refreshToken,
                additionResponse
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private OAuth2Authorization.Builder getAuthorizationBuilder(RegisteredClient registeredClient,
                                                                OAuth2ClientAuthenticationToken clientPrincipal,
                                                                Set<String> authorizedScopes) {
        return OAuth2Authorization.withRegisteredClient(registeredClient)
                .attribute(Principal.class.getName(), clientPrincipal)
                .principalName(clientPrincipal.getName())
                .authorizationGrantType(GRANT_TYPE)
                .authorizedScopes(authorizedScopes);
    }

    private DefaultOAuth2TokenContext.Builder getTokenContextBuilder(RegisteredClient registeredClient,
                                                                     OAuth2ClientAuthenticationToken clientPrincipal,
                                                                     Set<String> authorizedScopes,
                                                                     Authentication passwordAuthenticationToken) {
        return DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(clientPrincipal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(GRANT_TYPE)
                .authorizationGrant(passwordAuthenticationToken);
    }

    private OAuth2RefreshToken generateRefreshToken(OAuth2ClientAuthenticationToken clientAuthenticationToken) {
        return null;
    }

    private CustomPayloadValue getCustomPayloadValue(UserEntity userEntity, RegisteredClient registeredClient) {
        return CustomPayloadValue.builder()
                .userId(userEntity.getId())
                .userName(userEntity.getUsername())
                .scope(new HashSet<>())
                .authorities(new HashSet<>(Set.of("SYSTEM_ADMIN", "DESIGN")))
                .jti(null)
                .clientId(registeredClient.getClientId())
                .build();
    }

    private void validateUser(UserEntity userEntity,
                              String usernameRequest,
                              String passwordRequest) {
        if (!userEntity.getUsername().equals(usernameRequest)
                || !passwordEncoder.matches(passwordRequest, userEntity.getPassword())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.ACCESS_DENIED);
        }
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }
}

