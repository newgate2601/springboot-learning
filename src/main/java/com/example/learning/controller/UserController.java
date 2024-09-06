package com.example.learning.controller;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
// https://chatgpt.com/c/e5ad58ac-a248-495f-b8a1-c4168e647dfc
public class UserController {
    private final UserService userService;

    @PostMapping
    public Mono<UserEntity> create(@RequestBody Mono<UserSignUpRequest> userDto) {
        return userService.create(userDto);
    }

    @GetMapping
    public Mono<UserEntity> retrieve(@RequestParam int userId) {
        return userService.retrieve(userId);
    }

    @PutMapping
    public Mono<UserEntity> update(@RequestParam int userId, @RequestBody Mono<UserSignUpRequest> userDto) {
        return userService.update(userId, userDto);
    }

    @DeleteMapping
    public Mono<Void> delete(@RequestBody int userId) {
        return userService.delete(userId);
    }

    @GetMapping("/list")
    public Flux<UserEntity> list(ServerWebExchange exchange) {
        return userService.list(exchange);
    }
}
