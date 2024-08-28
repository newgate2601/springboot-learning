package com.example.learning.webflux;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("webflux/user")
@AllArgsConstructor
// https://chatgpt.com/c/e5ad58ac-a248-495f-b8a1-c4168e647dfc
public class UserController1 {
    private final UserService1 userService1;

    @PostMapping
    public Mono<UserEntity> create(@RequestBody Mono<UserSignUpRequest> userDto) {
        return userService1.create(userDto);
    }

    @GetMapping
    public Mono<UserEntity> retrieve(@RequestParam int userId) {
        return userService1.retrieve(userId);
    }

    @PutMapping
    public Mono<UserEntity> update(@RequestParam int userId, @RequestBody Mono<UserSignUpRequest> userDto) {
        return userService1.update(userId, userDto);
    }

    @DeleteMapping
    public Mono<Void> delete(@RequestBody int userId) {
        return userService1.delete(userId);
    }

    @GetMapping("/list")
    public Flux<UserEntity> list() {
        return userService1.list();
    }
}
