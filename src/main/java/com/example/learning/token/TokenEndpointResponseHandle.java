package com.example.learning.token;

import com.example.learning.dto.ClientRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class TokenEndpointResponseHandle implements AuthenticationSuccessHandler {
    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenResponseConverter
            = new OAuth2AccessTokenResponseHttpMessageConverter();
    private Consumer<OAuth2AccessTokenAuthenticationContext> accessTokenResponseCustomizer;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken accessTokenAuthentication)) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "Unable to process the access token response.", null);
            throw new OAuth2AuthenticationException(error);
        }

        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
        Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();
        Map<String, Object> newAdditionalParameters = new HashMap<>(additionalParameters);
        newAdditionalParameters.put("adzc", "kaka");
        newAdditionalParameters.put("adzc2", "kaka1");
        newAdditionalParameters.put("kaka", new ClientRequest("1", "2"));

        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                .tokenType(accessToken.getTokenType())
                .scopes(accessToken.getScopes());

        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        }
        if (refreshToken != null) {
            builder.refreshToken(refreshToken.getTokenValue());
        }
        if (!CollectionUtils.isEmpty(newAdditionalParameters)) {
            builder.additionalParameters(newAdditionalParameters);
        }

        if (this.accessTokenResponseCustomizer != null) {
            // @formatter:off
            OAuth2AccessTokenAuthenticationContext accessTokenAuthenticationContext =
                    OAuth2AccessTokenAuthenticationContext.with(accessTokenAuthentication)
                            .accessTokenResponse(builder)
                            .build();
            // @formatter:on
            this.accessTokenResponseCustomizer.accept(accessTokenAuthenticationContext);
        }

        OAuth2AccessTokenResponse accessTokenResponse = builder.build();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.accessTokenResponseConverter.write(accessTokenResponse, null, httpResponse);
    }
}
