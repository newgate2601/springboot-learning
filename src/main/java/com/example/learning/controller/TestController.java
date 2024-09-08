package com.example.learning.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oke")
@AllArgsConstructor
@CrossOrigin
public class TestController {

    @GetMapping
    public Integer test(){
        return 1;
    }
}
