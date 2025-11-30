package com.authservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AuthMicroserviceApplication {

	public static void main(String[] args) {
		//Load .env file and set system properties
		Dotenv dotenv = Dotenv.configure()
			.ignoreIfMissing()
			.load();

		dotenv.entries().forEach(entry -> 
		System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(AuthMicroserviceApplication.class, args);
	}

}
