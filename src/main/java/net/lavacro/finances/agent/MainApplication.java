package net.lavacro.finances.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MainApplication {

	static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}
}
