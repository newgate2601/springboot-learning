package com.example.learning.service;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    public Mono<UserEntity> create(Mono<UserSignUpRequest> userDto) {
        return userDto.map(userSignUpRequest -> userMapper.getEntityBy(userSignUpRequest))
                .flatMap(userRepository::save);
    }

    @Override
    public Mono<UserEntity> retrieve(int userId) {
        return userRepository.findById(Long.valueOf(userId));
    }

    @Override
    public Mono<UserEntity> update(int userId, Mono<UserSignUpRequest> userDto) {
        return userRepository.findById(Long.valueOf(userId))
                .flatMap(user -> userDto
                        .map(userMapper::getEntityBy)
                        .doOnNext(u -> u.setId(Long.valueOf(userId))))
                .flatMap(userRepository::save);
    }

    @Override
    public Mono<Void> delete(int userId) {
        return userRepository.deleteById(Long.valueOf(userId));
    }

    @Override
    public Flux<UserEntity> list(ServerWebExchange exchange) {
        System.out.println(exchange.getRequest().getHeaders().getFirst("abc"));
        return userRepository.findAll();
    }
}
