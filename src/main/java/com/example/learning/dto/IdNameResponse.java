package com.example.learning.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IdNameResponse {
    private Long id;
    private String name;
    private String password;
}
