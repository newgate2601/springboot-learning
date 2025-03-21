package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface UserService {
    List<UserEntity> getUsers();
    IdNameResponse signUp(UserSignUpRequest userSignUpRequest);
    UserEntity getUser(String username);
    UserDetails loadUserByUsername(String username);
}
