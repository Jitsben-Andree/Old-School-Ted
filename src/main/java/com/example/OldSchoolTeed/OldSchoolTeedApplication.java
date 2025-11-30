package com.example.OldSchoolTeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OldSchoolTeedApplication {

	public static void main(String[] args) {
		SpringApplication.run(OldSchoolTeedApplication.class, args);
	}

}
