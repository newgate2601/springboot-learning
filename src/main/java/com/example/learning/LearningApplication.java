package com.example.learning;

import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.Base64;

// https://www.javainuse.com/spring/springboot-oauth2-password-grant
// https://www.techgeeknext.com/spring-boot-security/springboot-oauth2-password-grant
// https://docs.spring.io/spring-authorization-server/reference/overview.html
// encode with secrete :) https://onexception.dev/news/1221772/spring-boot-oauth-bcrypt-client-secret
@RestController
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@EnableResourceServer
public class LearningApplication {
	public static void main(String[] args)	 {
		SpringApplication.run(LearningApplication.class, args);
	}

	// validate if Token is oke
	@RequestMapping("/validateUser")
	public Principal user(Principal user) {
		return user;
	}
}
