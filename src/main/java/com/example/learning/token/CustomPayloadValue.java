package com.example.learning.token;

import lombok.*;

import java.util.HashSet;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomPayloadValue {
    private Long userId;
    private String userName;
    private HashSet<String> scope;
    private HashSet<String> authorities;
    private String jti;
    private String clientId;
    private String ati;
}
