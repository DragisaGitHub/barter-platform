package com.barterplatform.web.catalog.storage;

import com.azure.core.exception.AzureException;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@Profile({"dev", "prod"})
public class AzureBlobStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);
    private static final String STORAGE_UNAVAILABLE_MESSAGE =
            "Image storage is currently unavailable. Please try again later.";
    private static final String NOT_AVAILABLE = "n/a";

    private final BlobContainerClient blobContainerClient;
    private final String containerName;

    public AzureBlobStorageService(
            BlobContainerClient blobContainerClient,
            @Value("${barter.storage.azure.container-name:${azure.storage.container-name:}}") String containerName) {
        this.blobContainerClient = blobContainerClient;
        this.containerName = containerName;
    }

    @Override
    public void store(String storageKey, InputStream inputStream, long contentLength, String contentType) throws IOException {
        long startedAt = System.nanoTime();
        try {
            byte[] content = inputStream.readAllBytes();
            BlobClient blobClient = blobContainerClient.getBlobClient(storageKey);
            blobClient.upload(new ByteArrayInputStream(content), content.length, false);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
            logSuccess("store", storageKey, (long) content.length, contentType, startedAt);
        } catch (BlobStorageException e) {
            logFailure("store", storageKey, contentLength, contentType, startedAt, e, e.getStatusCode(), valueOrDefault(e.getErrorCode()));
            throw storageUnavailable(e);
        } catch (RuntimeException e) {
            logFailure("store", storageKey, contentLength, contentType, startedAt, e, null, null);
            throw storageUnavailable(e);
        }
    }

    @Override
    public void delete(String storageKey) {
        long startedAt = System.nanoTime();
        try {
            blobContainerClient.getBlobClient(storageKey).deleteIfExists();
            logSuccess("delete", storageKey, null, null, startedAt);
        } catch (BlobStorageException e) {
            logFailure("delete", storageKey, null, null, startedAt, e, e.getStatusCode(), valueOrDefault(e.getErrorCode()));
            throw storageUnavailable(e);
        } catch (AzureException e) {
            logFailure("delete", storageKey, null, null, startedAt, e, null, null);
            throw storageUnavailable(e);
        } catch (RuntimeException e) {
            logFailure("delete", storageKey, null, null, startedAt, e, null, null);
            throw storageUnavailable(e);
        }
    }

    @Override
    public String resolveUrl(String storageKey) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/files/")
                    .path(storageKey)
                    .toUriString();
        } catch (IllegalStateException e) {
            return "/files/" + storageKey;
        }
    }

    @Override
    public StoredFile load(String storageKey) throws IOException {
        long startedAt = System.nanoTime();
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(storageKey);
            if (!blobClient.exists()) {
                log.info(
                        "Azure blob operation completed. operation={} containerName={} storageKey={} contentLength={} contentType={} elapsedMs={} result={}",
                        "load",
                        containerName,
                        storageKey,
                        NOT_AVAILABLE,
                        NOT_AVAILABLE,
                        elapsedMillis(startedAt),
                        "not_found");
                throw new NoSuchFileException(storageKey);
            }

            byte[] content = blobClient.downloadContent().toBytes();
            String contentType = blobClient.getProperties().getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            logSuccess("load", storageKey, (long) content.length, contentType, startedAt);
            return new StoredFile(content, contentType);
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                log.info(
                        "Azure blob operation completed. operation={} containerName={} storageKey={} contentLength={} contentType={} elapsedMs={} result={} statusCode={} errorCode={} message={}",
                        "load",
                        containerName,
                        storageKey,
                        NOT_AVAILABLE,
                        NOT_AVAILABLE,
                        elapsedMillis(startedAt),
                        "not_found",
                        e.getStatusCode(),
                        valueOrDefault(e.getErrorCode()),
                        sanitizeMessage(rootCauseMessage(e)));
                throw new NoSuchFileException(storageKey);
            }
            logFailure("load", storageKey, null, null, startedAt, e, e.getStatusCode(), valueOrDefault(e.getErrorCode()));
            throw storageUnavailable(e);
        } catch (AzureException e) {
            logFailure("load", storageKey, null, null, startedAt, e, null, null);
            throw storageUnavailable(e);
        } catch (RuntimeException e) {
            logFailure("load", storageKey, null, null, startedAt, e, null, null);
            throw storageUnavailable(e);
        }
    }

    private void logSuccess(String operation, String storageKey, Long contentLength, String contentType, long startedAt) {
        log.info(
                "Azure blob operation completed. operation={} containerName={} storageKey={} contentLength={} contentType={} elapsedMs={} result={}",
                operation,
                containerName,
                storageKey,
                valueOrDefault(contentLength),
                valueOrDefault(contentType),
                elapsedMillis(startedAt),
                "success");
    }

    private void logFailure(
            String operation,
            String storageKey,
            Long contentLength,
            String contentType,
            long startedAt,
            Throwable throwable,
            Integer statusCode,
            Object azureErrorCode) {
        log.error(
                "Azure blob operation failed. operation={} containerName={} storageKey={} contentLength={} contentType={} elapsedMs={} exceptionClass={} rootCauseClass={} statusCode={} errorCode={} message={}",
                operation,
                containerName,
                storageKey,
                valueOrDefault(contentLength),
                valueOrDefault(contentType),
                elapsedMillis(startedAt),
                throwable.getClass().getName(),
                rootCauseClass(throwable),
                statusCode != null ? statusCode : NOT_AVAILABLE,
                valueOrDefault(azureErrorCode),
                sanitizeMessage(rootCauseMessage(throwable)));
    }

    private ApiException storageUnavailable(Throwable cause) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, STORAGE_UNAVAILABLE_MESSAGE, cause);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String rootCauseClass(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getClass().getName();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage();
    }

    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return NOT_AVAILABLE;
        }

        return message
                .replaceAll("(?i)(AccountKey=)[^;\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)(SharedAccessSignature=)[^;\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)(sig=)[^&\\s]+", "$1[REDACTED]");
    }

    private String valueOrDefault(Object value) {
        if (value == null) {
            return NOT_AVAILABLE;
        }
        String text = value.toString();
        return (text == null || text.isBlank()) ? NOT_AVAILABLE : sanitizeMessage(text);
    }
}


