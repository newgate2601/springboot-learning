package com.example.learning.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@AllArgsConstructor
@CrossOrigin
public class Controller {
    @GetMapping("/")
    public String home() {
        return "Hello, public user!";
    }

    @GetMapping("/secure")
    public String secured() {
        return "Hello, logged in user!";
    }
}
