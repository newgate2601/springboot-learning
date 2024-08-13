package com.example.learning.controller;

import com.example.learning.dto.SeedDto;
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

    @GetMapping("/list")
    @Operation(summary = "Lấy danh sách user")
    public List<UserEntity> getUsers(){
        return userService.getUsers();
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Đăng ký")
    public SeedDto signUp(@RequestBody UserSignUpRequest userSignUpRequest){
        System.out.println("KAKAKAKAAKAKAKAK");
        return userService.signUp(userSignUpRequest);
    }
}
