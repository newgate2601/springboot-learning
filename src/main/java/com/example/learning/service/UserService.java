package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
//    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers(){
        List<UserEntity> userEntities = userRepository.findAll();
        return Objects.nonNull(userEntities) ? userEntities : new ArrayList<>();
    }

    @Transactional
    public IdNameResponse signUp(UserSignUpRequest userSignUpRequest){
        PasswordEncoder passwordEncoder1 = new BCryptPasswordEncoder();
        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
        userEntity.setPassword(passwordEncoder1.encode(userSignUpRequest.getPassword()));
        userRepository.save(userEntity);
        return IdNameResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getUsername())
                .password(userEntity.getPassword())
                .build();
    }
}
