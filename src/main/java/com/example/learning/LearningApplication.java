package com.example.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableScheduling
@EnableWebSecurity
// login http://localhost:8080/oauth2/authorization/google
// logout: https://accounts.google.com/Logout
// https://chatgpt.com/share/67a7d3d7-e120-8010-9cc8-1b69bc54f8fe
public class LearningApplication {

	public static void main(String[] args)	 {
		SpringApplication.run(LearningApplication.class, args);
	}
}
