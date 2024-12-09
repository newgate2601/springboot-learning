package com.example.learning.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VapidPublicKeyResponse {
    private String vapidPublicKey;
}
