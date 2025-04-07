package com.example.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// https://www.youtube.com/watch?v=gJZhdEJvZmc
// https://www.youtube.com/watch?v=hvYUwUmHB6M&t=378s
@SpringBootApplication
@EnableScheduling
public class LearningApplication {

	public static void main(String[] args)	 {
		SpringApplication.run(LearningApplication.class, args);
	}
}
