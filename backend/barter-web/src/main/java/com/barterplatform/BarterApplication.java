package com.barterplatform;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.barterplatform")
@EntityScan(basePackages = "com.barterplatform.domain.identity.entity")
@EnableJpaRepositories(basePackages = "com.barterplatform.infrastructure.identity.repository")
public class BarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarterApplication.class, args);
    }
}

