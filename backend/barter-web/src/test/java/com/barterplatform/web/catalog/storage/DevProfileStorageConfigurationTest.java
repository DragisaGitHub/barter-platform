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
            .withPropertyValues(
                    "spring.profiles.active=dev",
                    "azure.storage.connection-string=UseDevelopmentStorage=true",
                    "azure.storage.container-name=item-images-dev",
                    "storage.type=azure")
            .withUserConfiguration(StorageTestConfiguration.class);

    @Test
    void devProfileUsesAzureStorageServiceOnly() {
        contextRunner.run(applicationContext -> {
            FileStorageService storageService = applicationContext.getBean(FileStorageService.class);

            assertInstanceOf(AzureBlobStorageService.class, storageService);
            assertEquals(0, applicationContext.getBeansOfType(LocalFileStorageService.class).size());
            assertEquals(1, applicationContext.getBeansOfType(AzureBlobStorageService.class).size());
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

