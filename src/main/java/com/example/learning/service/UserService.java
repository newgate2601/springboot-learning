package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers(){
        return userJpaRepository.findAll();
    }

    @Transactional
    public IdNameResponse signUp(UserSignUpRequest userSignUpRequest){
        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        userJpaRepository.save(userEntity);
        return IdNameResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getUsername())
                .build();
    }

    public UserEntity getUser(String username) {
        return userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new User(userEntity.getUsername(), userEntity.getPassword(), new ArrayList<>());
    }
}
