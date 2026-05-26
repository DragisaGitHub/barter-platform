package com.barterplatform.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ProductionSecurityEnvironmentValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldFailFastWhenProdUsesUnsafeDefaults() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "barter.jwt.secret=default-dev-secret-key-that-is-at-least-32-bytes-long!!",
                        "barter.security.swagger-enabled=true",
                        "barter.email-verification.enabled=false",
                        "barter.storage.type=local")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("Unsafe production configuration")
                            .hasMessageContaining("must not use a placeholder or known DEV default")
                            .hasMessageContaining("Swagger/OpenAPI must be disabled in prod")
                            .hasMessageContaining("Email verification must stay enabled in prod")
                            .hasMessageContaining("barter.storage.type must be set to 'azure' in prod");
                });
    }

    @Test
    void shouldAllowSafeProdConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "barter.jwt.secret=prod-secret-value-with-at-least-32-characters!!",
                        "barter.security.swagger-enabled=false",
                        "barter.security.allowed-origins=https://barter-platform.example.com",
                        "barter.email-verification.enabled=true",
                        "spring.mail.host=smtp.example.com",
                        "barter.storage.type=azure",
                        "barter.storage.azure.connection-string=DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test;EndpointSuffix=core.windows.net",
                        "barter.storage.azure.container-name=item-images-prod")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldRejectLocalhostCorsOriginsInProd() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "barter.jwt.secret=prod-secret-value-with-at-least-32-characters!!",
                        "barter.security.swagger-enabled=false",
                        "barter.security.allowed-origins=http://localhost:5173",
                        "barter.email-verification.enabled=true",
                        "spring.mail.host=smtp.example.com",
                        "barter.storage.type=azure",
                        "barter.storage.azure.connection-string=DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test;EndpointSuffix=core.windows.net",
                        "barter.storage.azure.container-name=item-images-prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("must use HTTPS in prod")
                            .hasMessageContaining("must not point to localhost in prod");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityProperties.class)
    @Import(ProductionSecurityEnvironmentValidator.class)
    static class TestConfiguration {
    }
}

