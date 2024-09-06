package com.example.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class, ReactiveOAuth2ResourceServerAutoConfiguration.class})
// https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html
public class LearningApplication {

	public static void main(String[] args)	 {
		SpringApplication.run(LearningApplication.class, args);
	}
}
