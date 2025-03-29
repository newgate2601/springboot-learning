package com.example.learning.controller;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;

import com.example.learning.entity.UserEntity;
import com.example.learning.responsehandle.CustomResponse;
import com.example.learning.responsehandle.error.ErrorCodes;
import com.example.learning.responsehandle.exception.RequestException;
import com.example.learning.responsehandle.message.MessageService;
import com.example.learning.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
@CrossOrigin
public class UserController {
    private final UserService userService;
    private final MessageService messageService;

    @GetMapping("/list")
    @Operation(summary = "Lấy danh sách user")
    public List<UserEntity> getUsers(){
        return userService.getUsers();
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Đăng ký")
    public CustomResponse<IdNameResponse> signUp(@RequestBody UserSignUpRequest userSignUpRequest){
        return CustomResponse.ok(userService.signUp(userSignUpRequest));
    }

    @GetMapping("/message")
    public String getMessage(@RequestParam String key,
                             @RequestParam(required = false) String lang) {
        Locale locale = (lang != null && lang.equals("en")) ? Locale.ENGLISH : new Locale("vi");
        return messageService.getMessage(key, locale);
    }
}
