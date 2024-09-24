package com.example.learning.security_config.password_grant_type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

// GrantType(name)-Converter (pre-processor):
// extract credential from HttpServletRequest -> 1 OAuth2ClientAuthenticationToken instance
@Builder
public class PasswordAuthenticationConverter implements AuthenticationConverter {
    private final String PASSWORD_GRANT_TYPE = "custom_password";

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE); // "grant_type"
        if (!PASSWORD_GRANT_TYPE.equals(grantType)) {
            return null; // ignore provider of "password" grant type because not this grant type
        }

        MultiValueMap<String, String> parameters = getParameters(request);
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);

        // have scopes, split "read write delete" -> ["read", "write", "delete"]
        // don't have scopes -> null
        Set<String> requestedScopes = StringUtils.hasText(scope)
                ? new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")))
                : null;

        validateCredential(parameters, requestedScopes);

        Map<String, Object> credentialParams = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals(OAuth2ParameterNames.SCOPE)) {
                credentialParams.put(key, value.getFirst());
            }
        });

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        return new PasswordAuthenticationToken(clientPrincipal, requestedScopes, credentialParams);
    }

    private void validateCredential(MultiValueMap<String, String> parameters,
                                    Set<String> requestedScopes) {
        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username) ||
                parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        if (!StringUtils.hasText(password) ||
                parameters.get(OAuth2ParameterNames.PASSWORD).size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values.length > 0) {
                for (String value : values) {
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }
}
