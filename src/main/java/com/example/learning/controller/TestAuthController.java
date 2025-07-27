package com.example.learning.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestAuthController {

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello");
    }

    @PreAuthorize("hasRole('manager')")
    @GetMapping("/manager")
    public ResponseEntity<String> sayHelloToManager() {
        return ResponseEntity.ok("Hello manager");
    }

    @PreAuthorize("hasRole('member')")
    @GetMapping("/member")
    public ResponseEntity<String> sayHelloToMember() {
        return ResponseEntity.ok("Hello member");
    }
}
