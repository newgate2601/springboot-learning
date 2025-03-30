package com.example.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSignUpRequest {
    @NotBlank(message = "Tên không được để trống")
    private String username;
    @NotNull
    private String password;
}
