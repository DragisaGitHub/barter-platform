package com.barterplatform;

import static org.assertj.core.api.Assertions.assertThat;

import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!",
                "logging.level.org.flywaydb=DEBUG",
                "logging.level.org.springframework.boot.flyway=DEBUG",
                "logging.level.org.hibernate.tool.schema=DEBUG"
        }
)
@Testcontainers(disabledWithoutDocker = true)
class IdentityJpaBootstrapSmokeTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("barter_db")
            .withUsername("barter_user")
            .withPassword("barter_password");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextLoadsWithJpaRepositoriesAndFlyway() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBean(RoleRepository.class)).isSameAs(roleRepository);
        assertThat(applicationContext.getBean(UserRepository.class)).isSameAs(userRepository);

        assertThat(roleRepository.findByCode(RoleCode.USER)).isPresent();
        assertThat(roleRepository.findByCode(RoleCode.MODERATOR)).isPresent();
        assertThat(roleRepository.findByCode(RoleCode.ADMIN)).isPresent();
    }
}

