package com.barterplatform.web.catalog.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
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
            @Value("${azure.storage.connection-string}") String connectionString,
            HttpClient azureBlobHttpClient,
            RequestRetryOptions azureBlobRetryOptions) {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .httpClient(azureBlobHttpClient)
                .retryOptions(azureBlobRetryOptions)
                .buildClient();
    }

    @Bean
    BlobContainerClient blobContainerClient(
            BlobServiceClient blobServiceClient,
            @Value("${azure.storage.container-name}") String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }
}

