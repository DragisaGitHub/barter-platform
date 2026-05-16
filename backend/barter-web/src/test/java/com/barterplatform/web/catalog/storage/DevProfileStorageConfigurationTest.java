package com.barterplatform.web.catalog.storage;

import com.barterplatform.BarterApplication;
import com.barterplatform.application.catalog.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:5432/barter_db",
                "spring.datasource.username=barter_user",
                "spring.datasource.password=barter_password",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "azure.storage.connection-string=DefaultEndpointsProtocol=https;AccountName=devstoreaccount1;AccountKey=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==;EndpointSuffix=core.windows.net",
                "azure.storage.container-name=item-images-dev"
        }
)
@ActiveProfiles("dev")
class DevProfileStorageConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private FileStorageService storageService;

    @Test
    void devProfileUsesAzureStorageServiceOnly() {
        assertInstanceOf(AzureBlobStorageService.class, storageService);
        assertEquals(0, applicationContext.getBeansOfType(LocalFileStorageService.class).size());
        assertEquals(1, applicationContext.getBeansOfType(AzureBlobStorageService.class).size());
    }
}

