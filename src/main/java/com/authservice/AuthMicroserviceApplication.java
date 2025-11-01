package com.authservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AuthMicroserviceApplication {

	public static void main(String[] args) {
		/**
		 * AuthMicroserviceApplication.java 
		 * Making sure to load .env file first before Spring-Boot runs inorder not to leak any sensitive info in output.
		 */
		Dotenv dotenv = Dotenv.configure()
		.ignoreIfMissing()
		.load();

		dotenv.entries().forEach(entry -> 
		System.setProperty(entry.getKey(), entry.getValue())
		);
		
		SpringApplication.run(AuthMicroserviceApplication.class, args);
	}

}
