package com.barterplatform.web.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class AzureBlobStorageConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldFailFastWhenConnectionStringIsMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "barter.storage.azure.container-name=item-images-dev")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("barter.storage.azure.connection-string is required");
                });
    }

    @Test
    void shouldFailFastWhenContainerNameIsInvalid() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "barter.storage.azure.connection-string=UseDevelopmentStorage=true",
                        "barter.storage.azure.container-name=Item_Images_Dev")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("barter.storage.azure.container-name must be a valid Azure Blob container name");
                });
    }

    @Test
    void shouldSupportLegacyAzurePropertyAliases() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "azure.storage.connection-string=UseDevelopmentStorage=true",
                        "azure.storage.container-name=item-images-dev")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(BlobContainerClient.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AzureBlobStorageConfig.class)
    static class TestConfiguration {
    }
}

