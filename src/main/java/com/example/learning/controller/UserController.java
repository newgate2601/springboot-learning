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
    public Flux<UserEntity> list(ServerWebExchange exchange) throws InterruptedException {
//        Mono.deferContextual(context -> {
//            System.out.print("----------- isA: ");
//            System.out.println(context.getOrEmpty("isA"));
//            System.out.print("----------- isB: ");
//            System.out.println(context.getOrEmpty("isB"));
//            return Mono.just(0);
//        }).subscribe();
//        return userService.list(exchange);
        Thread.sleep(2000);
        return userService.list(exchange)
                .doOnEach(signal -> {
                    signal.getContextView().getOrEmpty("isA").ifPresent(isA -> {
                        System.out.print("----------- isA: ");
                        System.out.println(isA);
                    });

                    signal.getContextView().getOrEmpty("isB").ifPresent(isB -> {
                        System.out.print("----------- isB: ");
                        System.out.println(isB);
                    });
                });
    }
}
