package com.example.learning.security_config.password_grant_type;

import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

@Getter
public class PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;
    private final Set<String> scopes;

    public PasswordAuthenticationToken(Authentication clientPrincipal,
                                       @Nullable Set<String> scopes,
                                       @Nullable Map<String, Object> credentialParams) {
        super(new AuthorizationGrantType("custom_password"), clientPrincipal, credentialParams);
        assert credentialParams != null;
        this.username = (String) credentialParams.get("username");
        this.password = (String) credentialParams.get("password");
        this.scopes = Collections.unmodifiableSet(
                scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
    }

}
