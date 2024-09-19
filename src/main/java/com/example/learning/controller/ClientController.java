package com.example.learning.controller;

import com.example.learning.dto.ClientRequest;
import com.example.learning.entity.ClientEntity;
import com.example.learning.repository.client.ClientJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client")
@AllArgsConstructor
@CrossOrigin
public class ClientController {
    private final PasswordEncoder passwordEncoder;
    private final ClientJpaRepository clientJpaRepository;

    @PostMapping
    public void create(@RequestBody ClientRequest clientRequest) {
        clientJpaRepository.save(
                ClientEntity.builder()
                        .clientId(clientRequest.getClientId())
                        .clientSecret(passwordEncoder.encode(clientRequest.getClientSecret()))
                        .build()
        );
    }
}
