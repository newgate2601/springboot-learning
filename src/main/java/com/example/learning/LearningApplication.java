package com.example.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Hooks;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableScheduling
public class LearningApplication {
	public static void main(String[] args)	 {
		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(LearningApplication.class, args);
	}
}
