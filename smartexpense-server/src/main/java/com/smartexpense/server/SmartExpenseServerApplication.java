package com.smartexpense.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartExpenseServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartExpenseServerApplication.class, args);
    }
}
