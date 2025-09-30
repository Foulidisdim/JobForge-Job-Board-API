package com.jobforge.jobboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JobForgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobForgeApplication.class, args);
	}

}
