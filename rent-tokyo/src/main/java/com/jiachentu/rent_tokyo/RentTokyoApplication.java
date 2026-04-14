package com.jiachentu.rent_tokyo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RentTokyoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentTokyoApplication.class, args);
	}

}
