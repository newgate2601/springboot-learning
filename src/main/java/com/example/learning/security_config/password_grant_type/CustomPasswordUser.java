package com.example.learning.security_config.password_grant_type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@AllArgsConstructor
@Getter
@Setter
public class CustomPasswordUser {
    private String username;
    private Collection<GrantedAuthority> authorities;
}
