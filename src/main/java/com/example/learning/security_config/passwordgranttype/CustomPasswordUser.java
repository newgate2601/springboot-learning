package com.example.learning.security_config.passwordgranttype;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class CustomPasswordUser {
    private String username;
    private Collection<GrantedAuthority> authorities;
}
