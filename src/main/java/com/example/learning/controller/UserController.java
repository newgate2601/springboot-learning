package com.example.learning.controller;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.service.UserService;
import com.example.learning.service.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin
public class UserController {
    @Autowired
    private UserService userServiceImpl;

    @GetMapping("/list")
    @Operation(summary = "Lấy danh sách user")
    public List<UserEntity> getUsers(){
        return userServiceImpl.getUsers();
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Đăng ký")
    public IdNameResponse signUp(@RequestBody UserSignUpRequest userSignUpRequest){
        return userServiceImpl.signUp(userSignUpRequest);
    }
}
