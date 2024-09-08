package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers(){
        String url = "http://localhost:8080/oke/";
        Integer value = restTemplate.getForObject(url, Integer.class);
        System.out.println("************ : " + value);
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
