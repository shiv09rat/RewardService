package com.retailer.rewardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Retailer Reward Service Spring Boot application.
 * Exposes RESTful APIs to calculate customer reward points based on purchase history.
 */
@SpringBootApplication
public class RewardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardServiceApplication.class, args);
    }
}
