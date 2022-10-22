package com.zeeforum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zeeforum.mapper")
public class ZeeforumApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZeeforumApplication.class, args);
	}

}