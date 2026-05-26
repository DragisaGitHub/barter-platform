package com.barterplatform.web.catalog.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "prod"})
public class AzureBlobStorageConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(20);

    private static final int MAX_TRIES = 2;
    private static final int TRY_TIMEOUT_SECONDS = 20;
    private static final long RETRY_DELAY_MILLIS = 500L;
    private static final long MAX_RETRY_DELAY_MILLIS = 2_000L;
    private static final Pattern CONTAINER_NAME_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$");

    @Bean
    HttpClient azureBlobHttpClient() {
        return new NettyAsyncHttpClientBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .responseTimeout(RESPONSE_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .writeTimeout(WRITE_TIMEOUT)
                .build();
    }

    @Bean
    RequestRetryOptions azureBlobRetryOptions() {
        return new RequestRetryOptions(
                RetryPolicyType.EXPONENTIAL,
                MAX_TRIES,
                TRY_TIMEOUT_SECONDS,
                RETRY_DELAY_MILLIS,
                MAX_RETRY_DELAY_MILLIS,
                null);
    }

    @Bean
    BlobServiceClient blobServiceClient(
            @Value("${barter.storage.azure.connection-string:${azure.storage.connection-string:}}") String connectionString,
            HttpClient azureBlobHttpClient,
            RequestRetryOptions azureBlobRetryOptions) {
        String validatedConnectionString = requireConnectionString(connectionString);

        try {
            return new BlobServiceClientBuilder()
                    .connectionString(validatedConnectionString)
                    .httpClient(azureBlobHttpClient)
                    .retryOptions(azureBlobRetryOptions)
                    .buildClient();
        } catch (IllegalArgumentException ex) {
            throw new ApplicationContextException(
                    "barter.storage.azure.connection-string is invalid. "
                            + "Set AZURE_STORAGE_CONNECTION_STRING_DEV or AZURE_STORAGE_CONNECTION_STRING for dev, "
                            + "and AZURE_STORAGE_CONNECTION_STRING_PROD or AZURE_STORAGE_CONNECTION_STRING for prod.",
                    ex);
        }
    }

    @Bean
    BlobContainerClient blobContainerClient(
            BlobServiceClient blobServiceClient,
            @Value("${barter.storage.azure.container-name:${azure.storage.container-name:}}") String containerName) {
        return blobServiceClient.getBlobContainerClient(requireContainerName(containerName));
    }

    private String requireConnectionString(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new ApplicationContextException(
                    "barter.storage.azure.connection-string is required when the dev or prod profile uses Azure image storage. "
                            + "Set AZURE_STORAGE_CONNECTION_STRING_DEV or AZURE_STORAGE_CONNECTION_STRING for dev, "
                            + "and AZURE_STORAGE_CONNECTION_STRING_PROD or AZURE_STORAGE_CONNECTION_STRING for prod.");
        }
        return connectionString.trim();
    }

    private String requireContainerName(String containerName) {
        if (containerName == null || containerName.isBlank()) {
            throw new ApplicationContextException(
                    "barter.storage.azure.container-name is required when the dev or prod profile uses Azure image storage. "
                            + "Set AZURE_STORAGE_CONTAINER_DEV or AZURE_STORAGE_CONTAINER for dev, "
                            + "and AZURE_STORAGE_CONTAINER_PROD or AZURE_STORAGE_CONTAINER for prod.");
        }

        String normalized = containerName.trim();
        if (normalized.length() < 3
                || normalized.length() > 63
                || normalized.contains("--")
                || !CONTAINER_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ApplicationContextException(
                    "barter.storage.azure.container-name must be a valid Azure Blob container name "
                            + "(3-63 lowercase letters, numbers, and hyphens; start/end with a letter or number; no consecutive hyphens).");
        }
        return normalized;
    }
}

