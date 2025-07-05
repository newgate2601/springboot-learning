package com.example.learning.controller;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UpdateUserRequest;
import com.example.learning.dto.UserSignUpRequest;

import com.example.learning.entity.UserEntity;
import com.example.learning.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
@CrossOrigin
public class UserController {
    private final UserService userService;

    @PutMapping
    public void updateUser(Long userId, UpdateUserRequest updateUserRequest, Boolean isSleep){
        userService.updateUser(userId, updateUserRequest, isSleep);
    }

    @GetMapping("/list")
    @Operation(summary = "Lấy danh sách user")
    public List<UserEntity> getUsers(){
        return userService.getUsers();
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Đăng ký")
    public IdNameResponse signUp(@RequestBody UserSignUpRequest userSignUpRequest){
        return userService.signUp(userSignUpRequest);
    }
}
