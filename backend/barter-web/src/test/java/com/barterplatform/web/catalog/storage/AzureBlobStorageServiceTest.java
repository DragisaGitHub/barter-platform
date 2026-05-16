package com.barterplatform.web.catalog.storage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.common.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.nio.file.NoSuchFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobStorageServiceTest {

    private static final String CONTAINER_NAME = "item-images-dev";

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    private AzureBlobStorageService service() {
        return new AzureBlobStorageService(blobContainerClient, CONTAINER_NAME);
    }

    @Test
    void storeUploadsBlobWithoutOverwrite() throws Exception {
        AzureBlobStorageService service = service();
        byte[] content = "blob-data".getBytes();
        String storageKey = "items/demo/test.jpg";

        when(blobContainerClient.getBlobClient(storageKey)).thenReturn(blobClient);

        service.store(storageKey, new ByteArrayInputStream(content), content.length, "image/jpeg");

        verify(blobClient).upload(any(ByteArrayInputStream.class), eq((long) content.length), eq(false));
        verify(blobClient).setHttpHeaders(any());
    }

    @Test
    void deleteDelegatesToBlobClient() {
        AzureBlobStorageService service = service();
        String storageKey = "items/demo/test.jpg";

        when(blobContainerClient.getBlobClient(storageKey)).thenReturn(blobClient);

        service.delete(storageKey);

        verify(blobClient).deleteIfExists();
    }

    @Test
    void loadReturnsBlobBytesAndContentType() throws Exception {
        AzureBlobStorageService service = service();
        String storageKey = "items/demo/test.jpg";
        byte[] content = "blob-data".getBytes();

        when(blobContainerClient.getBlobClient(storageKey)).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromBytes(content));
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getContentType()).thenReturn("image/jpeg");

        FileStorageService.StoredFile stored = service.load(storageKey);

        assertArrayEquals(content, stored.content());
        assertEquals("image/jpeg", stored.contentType());
    }

    @Test
    void loadThrowsWhenBlobDoesNotExist() {
        AzureBlobStorageService service = service();
        String storageKey = "items/demo/missing.jpg";

        when(blobContainerClient.getBlobClient(storageKey)).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThrows(NoSuchFileException.class, () -> service.load(storageKey));
        verify(blobClient, never()).downloadContent();
    }

    @Test
    void storeConvertsBlobStorageExceptionToGenericApiExceptionAndSanitizesLogs() {
        AzureBlobStorageService service = service();
        String storageKey = "items/demo/test.jpg";
        String secret = "super-secret-account-key";
        BlobStorageException blobStorageException = mock(BlobStorageException.class);

        when(blobContainerClient.getBlobClient(storageKey)).thenReturn(blobClient);
        doThrow(blobStorageException).when(blobClient).upload(any(ByteArrayInputStream.class), eq(9L), eq(false));
        when(blobStorageException.getStatusCode()).thenReturn(503);
        when(blobStorageException.getErrorCode()).thenReturn(null);
        when(blobStorageException.getMessage()).thenReturn(
                "Upload failed DefaultEndpointsProtocol=https;AccountName=barterdevstorage784;AccountKey="
                        + secret
                        + ";EndpointSuffix=core.windows.net");

        Logger logger = (Logger) LoggerFactory.getLogger(AzureBlobStorageService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        try {
            ApiException ex = assertThrows(ApiException.class,
                    () -> service.store(storageKey, new ByteArrayInputStream("blob-data".getBytes()), 9L, "image/jpeg"));

            assertEquals("Image storage is currently unavailable. Please try again later.", ex.getMessage());

            String combinedLogs = listAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertTrue(combinedLogs.contains("operation=store"));
            assertTrue(combinedLogs.contains("containerName=" + CONTAINER_NAME));
            assertTrue(combinedLogs.contains("statusCode=503"));
            assertTrue(combinedLogs.contains("exceptionClass=com.azure.storage.blob.models.BlobStorageException"));
            assertTrue(combinedLogs.contains("AccountKey=[REDACTED]"));
            assertFalse(combinedLogs.contains(secret));
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}

