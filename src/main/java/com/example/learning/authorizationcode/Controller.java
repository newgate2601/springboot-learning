package com.example.learning.authorizationcode;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
@AllArgsConstructor
public class Controller {
    @GetMapping
    public String a(){
        return "Hello World";
    }

    @GetMapping("/oke")
    public String b(){
        return "Hello World oke";
    }
}
