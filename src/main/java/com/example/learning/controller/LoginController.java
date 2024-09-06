package com.example.learning.controller;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/login")
@AllArgsConstructor
public class LoginController {
    @PostMapping
    public Mono<String> login() {
        return Mono.just("Ok!!");
    }
}
