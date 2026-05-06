package com.javeme.duobao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DuoBaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuoBaoApplication.class, args);
    }

}
