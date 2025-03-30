package com.example.learning.controller;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.responsehandle.message.MessageService;
import com.example.learning.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
    public List<UserEntity> getUsers() {
        return userService.getUsers();
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Đăng ký")
    public IdNameResponse signUp(@RequestBody @Valid UserSignUpRequest userSignUpRequest) {
        return userService.signUp(userSignUpRequest);
    }

    @GetMapping("/message")
    public IdNameResponse getMessage(@RequestParam String key,
                                     @RequestParam(required = false) String lang) {
        Locale locale = (lang != null && lang.equals("en")) ? Locale.ENGLISH : new Locale("vi");
        return IdNameResponse.builder()
                .id(1L)
                .name(messageService.getMessage(key, locale))
                .build();
    }

//    @GetMapping("/message")
//    public String getMessage(@RequestParam String key,
//                                     @RequestParam(required = false) String lang) {
//        Locale locale = (lang != null && lang.equals("en")) ? Locale.ENGLISH : new Locale("vi");
//        return messageService.getMessage(key, locale);
//    }
}
