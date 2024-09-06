package com.example.learning.service;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserEntity> create(Mono<UserSignUpRequest> userDto);

    Mono<UserEntity> retrieve(int userId);

    Mono<UserEntity> update(int userId, Mono<UserSignUpRequest> userDto);

    Mono<Void> delete(int userId);

    Flux<UserEntity> list(ServerWebExchange exchange);
}
