package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserEntity> get(Long id) {
        UserEntity userEntity = userRepository.findById(id).orElse(null);
        log.error("After get 1 with username: " + userEntity.getUsername());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        entityManager.clear();
        List<UserEntity> userEntities = userRepository.findAll();
        return userEntities;
    }

    @Transactional
    public void update(Long id) {
        UserEntity userEntity = userRepository.findById(id).orElse(null);
//        log.error("After get 1 !!!");
        userEntity.setSeed("keke1");
        userEntity.setUsername("1111");
        userRepository.save(userEntity);
        entityManager.flush();
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        log.error("After update 1 !!!");
    }

    @Transactional(readOnly = true)
    public UserEntity get2(Long id) {
        UserEntity userEntity = userRepository.findById(id).orElse(null);
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        log.error("After get 2 !!!");
        return userEntity;
    }

    @Transactional
    public void update2(Long id) {
        UserEntity userEntity = userRepository.findById(id).orElse(null);
//        log.error("After get 1 !!!");
        userEntity.setSeed("keke2");
        userEntity.setUsername("2222");
        userRepository.save(userEntity);
        entityManager.flush();
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        log.error("After update 2 !!!");
    }

    @Transactional
    public List<UserEntity> getUsers(){
        List<UserEntity> userEntities = userRepository.findAll();
        Map<Long, UserEntity> map = userEntities.stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        UserEntity userEntity = map.get(14L);
        userEntity.setUsername("2222");
        userRepository.save(userEntity);
        userRepository.save(new UserEntity());
        entityManager.flush();
        log.error("After update 2 !!!");
        System.out.println(userEntities);
        return Objects.nonNull(userEntities) ? userEntities : new ArrayList<>();
    }

    @Transactional
    public IdNameResponse signUp(UserSignUpRequest userSignUpRequest){
        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
        userRepository.save(userEntity);
        return IdNameResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getUsername())
                .build();
    }
}
