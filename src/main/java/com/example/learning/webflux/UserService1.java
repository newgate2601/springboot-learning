package com.example.learning.webflux;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService1 {
    Mono<UserEntity> create(Mono<UserSignUpRequest> userDto);

    Mono<UserEntity> retrieve(int userId);

    Mono<UserEntity> update(int userId, Mono<UserSignUpRequest> userDto);

    Mono<Void> delete(int userId);

    Flux<UserEntity> list();
}
