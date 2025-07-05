package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UpdateUserRequest;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @SneakyThrows
    public void updateUser(Long userId, UpdateUserRequest updateUserRequest, Boolean isSleep) {
        UserEntity userEntity = userRepository.findById(userId).get();
        String threadName = Thread.currentThread().getName();
        userMapper.updateEntity(userEntity, updateUserRequest);
        log.info(threadName + "Prepare update user");
        userRepository.save(userEntity);
        userRepository.flush();
        if (isSleep) {
            log.info(threadName + "Sleeping for 20 seconds");
            Thread.sleep(20000);
            log.info(threadName + "Wakeup after 20 seconds");
        }
        log.info(threadName + "User updated {}", updateUserRequest);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers(){
        List<UserEntity> userEntities = userRepository.findAll();
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
