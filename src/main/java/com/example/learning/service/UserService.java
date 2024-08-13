package com.example.learning.service;

import com.example.learning.dto.SeedDto;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.AllArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers(){
        List<UserEntity> userEntities = userRepository.findAll();
        return Objects.nonNull(userEntities) ? userEntities : new ArrayList<>();
    }

    @Transactional
    public SeedDto signUp(UserSignUpRequest userSignUpRequest){
        String seed = generateSeed();
        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
        userEntity.setSeed(seed);
        userRepository.save(userEntity);
        return new SeedDto(seed);
    }

    private String generateSeed(){
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }
}
