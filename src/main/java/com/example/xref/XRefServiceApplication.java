package com.example.xref;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XRefServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(XRefServiceApplication.class, args);
    }
}