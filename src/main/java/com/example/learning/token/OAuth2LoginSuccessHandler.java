package com.example.learning.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private static final String GOOGLE = "google";

    public OAuth2LoginSuccessHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Chỉ áp dụng cho Google OAuth2
            if (GOOGLE.equalsIgnoreCase(registrationId)) {
                // Custom response
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("message", "Hello, public user!");
                responseData.put("user", authentication.getName());

                response.getWriter().write(objectMapper.writeValueAsString(responseData));
                response.getWriter().flush();
                return; // Ngăn chặn điều hướng mặc định
            }
        }
        response.sendRedirect("/");
    }
}
