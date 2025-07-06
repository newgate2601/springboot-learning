package com.example.learning.controller;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.keycloak.KeycloakTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/keycloak")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class KeycloakController {
    private final KeycloakTokenService keycloakTokenService;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    // ************************************* Password grant type *************************************
    @PostMapping("/login")
    public String getAccessToken(@RequestBody UserSignUpRequest userSignUpRequest) {
        return keycloakTokenService.getAccessToken(userSignUpRequest.getUsername(), userSignUpRequest.getPassword());
    }

    // ************************************* Authorization code grant type *************************************
    // API 1: Lấy authorization code
    @GetMapping("/login-url")
    public String getLoginUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/auth" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=http://localhost:8081/api/v1/keycloak/callback" +
                "&scope=openid profile email";
    }

    // API 2: call back từ authorization code lấy access token
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        // 1. Chuẩn bị URL và request body
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code); // Phải khớp với redirect_uri đăng ký trong Keycloak
        body.add("redirect_uri", "http://localhost:8081/api/v1/keycloak/callback"); // URI đặc biệt

        // 2. Gọi API Keycloak để lấy token
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.postForObject(
                    tokenUrl,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            // 3. Chỉ trả về access token
            String accessToken = (String) response.get("access_token");
            return ResponseEntity.ok(accessToken);

        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        }
    }

}
