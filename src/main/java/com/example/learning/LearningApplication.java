package com.example.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearningApplication {
// https://chatgpt.com/c/671e2560-5554-8010-85d7-2ef7d8cf7c5d
	public static void main(String[] args)	 {
		SpringApplication.run(LearningApplication.class, args);
	}
}

//curl --location 'localhost:8086/api/v1/user/list' \
//		--header 'Authorization: 2'
