package com.barterplatform;

import com.barterplatform.web.bootstrap.AdminBootstrapProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.barterplatform")
@EntityScan(basePackages = {
        "com.barterplatform.domain.identity.entity",
        "com.barterplatform.domain.catalog.entity",
        "com.barterplatform.domain.catalog.moderation",
        "com.barterplatform.domain.moderation.report",
        "com.barterplatform.domain.trade.entity",
        "com.barterplatform.domain.reputation.entity",
        "com.barterplatform.domain.notification.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.barterplatform.infrastructure.identity.repository",
        "com.barterplatform.infrastructure.catalog.repository",
        "com.barterplatform.infrastructure.moderation.repository",
        "com.barterplatform.infrastructure.trade.repository",
        "com.barterplatform.infrastructure.reputation.repository",
        "com.barterplatform.infrastructure.notification.repository"
})
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class BarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarterApplication.class, args);
    }
}

