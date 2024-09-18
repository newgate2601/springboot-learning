package com.example.learning.controller;

import com.example.learning.dto.ClientRequest;
import com.example.learning.entity.ClientEntity;
import com.example.learning.repository.client.ClientRepository;
import com.example.learning.repository.client.JpaRegisteredClientRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api/v1/client")
@AllArgsConstructor
@CrossOrigin
public class ClientController {
    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;

    @PostMapping
    public void create(@RequestBody ClientRequest clientRequest) {
        clientRepository.save(
                ClientEntity.builder()
                        .clientId(clientRequest.getClientId())
                        .clientSecret(passwordEncoder.encode(clientRequest.getClientSecret()))
                        .build()
        );
    }
}
