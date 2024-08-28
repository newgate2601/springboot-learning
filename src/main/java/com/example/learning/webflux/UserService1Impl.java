package com.example.learning.webflux;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserService1Impl implements UserService1 {

    @Autowired
    private UserRepository userRepository;
    private UserMapper userMapper;


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
    public Flux<UserEntity> list() {
        System.out.println("---------------------");
        Flux<UserEntity> userEntityFlux = userRepository.findAll();
        System.out.println("---------------------");
        return userEntityFlux;
    }
}
