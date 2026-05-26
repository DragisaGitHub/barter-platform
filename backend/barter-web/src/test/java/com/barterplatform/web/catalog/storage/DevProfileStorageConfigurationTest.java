package com.barterplatform.web.catalog.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.barterplatform.application.catalog.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class DevProfileStorageConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageTestConfiguration.class);

    @Test
    void devProfileUsesAzureStorageServiceOnly() {
        contextRunner.withPropertyValues(
                "spring.profiles.active=dev",
                "barter.storage.type=azure",
                "barter.storage.azure.connection-string=UseDevelopmentStorage=true",
                "barter.storage.azure.container-name=item-images-dev").run(applicationContext -> {
            FileStorageService storageService = applicationContext.getBean(FileStorageService.class);

            assertInstanceOf(AzureBlobStorageService.class, storageService);
            assertEquals(0, applicationContext.getBeansOfType(LocalFileStorageService.class).size());
            assertEquals(1, applicationContext.getBeansOfType(AzureBlobStorageService.class).size());
            assertEquals(1, applicationContext.getBeansOfType(FileStorageService.class).size());
        });
    }

    @Test
    void prodProfileUsesAzureStorageServiceOnly() {
        contextRunner.withPropertyValues(
                "spring.profiles.active=prod",
                "barter.storage.type=azure",
                "barter.storage.azure.connection-string=DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test;EndpointSuffix=core.windows.net",
                "barter.storage.azure.container-name=item-images-prod").run(applicationContext -> {
            FileStorageService storageService = applicationContext.getBean(FileStorageService.class);

            assertInstanceOf(AzureBlobStorageService.class, storageService);
            assertEquals(0, applicationContext.getBeansOfType(LocalFileStorageService.class).size());
            assertEquals(1, applicationContext.getBeansOfType(AzureBlobStorageService.class).size());
            assertEquals(1, applicationContext.getBeansOfType(FileStorageService.class).size());
        });
    }

    @Test
    void localProfileUsesLocalStorageServiceOnly() {
        contextRunner.withPropertyValues(
                "spring.profiles.active=local",
                "barter.storage.type=local",
                "barter.storage.local.base-path=build/test-uploads").run(applicationContext -> {
            FileStorageService storageService = applicationContext.getBean(FileStorageService.class);

            assertInstanceOf(LocalFileStorageService.class, storageService);
            assertEquals(1, applicationContext.getBeansOfType(LocalFileStorageService.class).size());
            assertEquals(0, applicationContext.getBeansOfType(AzureBlobStorageService.class).size());
            assertEquals(1, applicationContext.getBeansOfType(FileStorageService.class).size());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({AzureBlobStorageService.class, LocalFileStorageService.class})
    static class StorageTestConfiguration {

        @Bean
        BlobContainerClient blobContainerClient() {
            return mock(BlobContainerClient.class);
        }
    }
}

