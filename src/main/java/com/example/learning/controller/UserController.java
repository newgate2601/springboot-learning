package com.example.learning.controller;

import com.example.learning.dto.IdNameResponse;
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

    @GetMapping("/1")
    @Operation(summary = "Get1")
    public List<UserEntity> get(@RequestParam Long id) {
        return userService.get(id);
    }

    @GetMapping("/2")
    @Operation(summary = "Get2")
    public UserEntity get2(@RequestParam Long id) {
        return userService.get2(id);
    }

    @PutMapping("/1")
    @Operation(summary = "Update1")
    public void update(@RequestParam Long id) {
        userService.update(id);
    }

    @PutMapping("/2")
    @Operation(summary = "Update2")
    public void update2(@RequestParam Long id) {
        userService.update2(id);
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
