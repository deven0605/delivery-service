package com.thalicloud.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling — M4.1: the OFFERED-assignment expiry sweep (FR-4.3/FR-4.5).
@SpringBootApplication
@EnableScheduling
public class DeliveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
