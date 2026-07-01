package com.barterplatform.web.admin.service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import com.barterplatform.api.model.AdminOperationsBackupsResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Provides backup status information for the Admin Operations Center.
 *
 * <p>When {@code BACKUP_AZURE_CONNECTION_STRING}, {@code BACKUP_AZURE_CONTAINER}, and
 * {@code BACKUP_AZURE_PREFIX} are configured, this service connects to Azure Blob Storage,
 * lists blobs under the configured prefix, and returns metadata for the newest
 * {@code .dump.gz} backup file found.
 *
 * <p>The Azure connection string is <strong>never</strong> logged or included in any response.
 * A safe placeholder is returned when configuration is missing or when the Azure listing fails.
 */
@Service
public class AdminOperationsBackupsService {

    private static final Logger log = LoggerFactory.getLogger(AdminOperationsBackupsService.class);

    private static final String AVAILABILITY_CONFIGURED = "configured";
    private static final String AVAILABILITY_UNAVAILABLE = "unavailable";
    private static final String AVAILABILITY_PLACEHOLDER = "placeholder";
    private static final String STORAGE_PROVIDER = "azure-blob";

    /**
     * Matches the timestamp segment in backup filenames.
     * Example: {@code barter-barter_db-20260701T194213Z.dump.gz} → captures {@code 20260701T194213Z}.
     */
    private static final Pattern BLOB_TIMESTAMP_PATTERN =
            Pattern.compile("(\\d{8}T\\d{6}Z)\\.dump\\.gz$");

    private static final DateTimeFormatter BLOB_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    /**
     * Single attempt with 15-second try-timeout. This is a read-only admin operation;
     * we do not want slow hanging calls blocking the endpoint.
     */
    private static final RequestRetryOptions RETRY_OPTIONS =
            new RequestRetryOptions(RetryPolicyType.EXPONENTIAL, 1, 15, null, null, null);

    private final String connectionString;
    private final String container;
    private final String prefix;
    private final boolean backupEnabled;

    public AdminOperationsBackupsService(
            @Value("${barter.backup.azure.connection-string:}") String connectionString,
            @Value("${barter.backup.azure.container:}") String container,
            @Value("${barter.backup.azure.prefix:}") String prefix,
            @Value("${barter.backup.enabled:false}") boolean backupEnabled) {
        this.connectionString = connectionString;
        this.container = container;
        this.prefix = prefix;
        this.backupEnabled = backupEnabled;
    }

    public AdminOperationsBackupsResponse getBackups() {
        if (isBlank(connectionString) || isBlank(container)) {
            log.debug("Backup Azure configuration is absent; returning placeholder backup status.");
            return placeholder();
        }

        try {
            return queryAzureBackups();
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "Backup Azure configuration is invalid: exceptionClass={} message={}",
                    ex.getClass().getName(),
                    sanitizeMessage(ex.getMessage()));
            return unavailable("Backup storage configuration is invalid.");
        } catch (Exception ex) {
            log.warn(
                    "Backup Azure listing failed: exceptionClass={} rootCause={} message={}",
                    ex.getClass().getName(),
                    rootCauseClass(ex),
                    sanitizeMessage(rootCauseMessage(ex)));
            return unavailable("Backup listing is currently unavailable.");
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private AdminOperationsBackupsResponse queryAzureBackups() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .retryOptions(RETRY_OPTIONS)
                .buildClient();

        BlobContainerClient containerClient = serviceClient.getBlobContainerClient(container);

        String listPrefix = isBlank(prefix) ? null
                : (prefix.endsWith("/") ? prefix : prefix + "/");

        ListBlobsOptions options = new ListBlobsOptions()
                .setDetails(new BlobListDetails().setRetrieveMetadata(false))
                .setPrefix(listPrefix);

        List<BlobItem> dumpBlobs = StreamSupport
                .stream(containerClient.listBlobs(options, null).spliterator(), false)
                .filter(b -> b.getName().endsWith(".dump.gz"))
                .toList();

        if (dumpBlobs.isEmpty()) {
            log.debug("No .dump.gz backup blobs found under container={} prefix={}", container, prefix);
            return new AdminOperationsBackupsResponse()
                    .availability(AVAILABILITY_CONFIGURED)
                    .container(container)
                    .prefix(isBlank(prefix) ? null : prefix)
                    .storageProvider(STORAGE_PROVIDER)
                    .scheduledBackupEnabled(backupEnabled)
                    .note("No backup blobs found under the configured prefix.");
        }

        // Find newest: prefer filename-parsed timestamp, fall back to Azure blob lastModified.
        BlobItem newest = dumpBlobs.stream()
                .max(Comparator.comparing(this::effectiveInstant))
                .orElseThrow();

        OffsetDateTime blobLastModified = newest.getProperties().getLastModified();
        Long sizeBytes = newest.getProperties().getContentLength();

        Optional<Instant> parsedTimestamp = parseTimestampFromName(newest.getName());
        OffsetDateTime lastBackupTimestamp = parsedTimestamp
                .map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC))
                .orElse(blobLastModified);

        log.debug(
                "Latest backup blob resolved: name={} sizeBytes={} lastModified={}",
                newest.getName(), sizeBytes, blobLastModified);

        return new AdminOperationsBackupsResponse()
                .availability(AVAILABILITY_CONFIGURED)
                .blobName(newest.getName())
                .lastBackupTimestamp(lastBackupTimestamp)
                .blobLastModified(blobLastModified)
                .sizeBytes(sizeBytes)
                .container(container)
                .prefix(isBlank(prefix) ? null : prefix)
                .storageProvider(STORAGE_PROVIDER)
                .scheduledBackupEnabled(backupEnabled);
    }

    /**
     * Effective instant for sorting: filename-parsed timestamp if available,
     * otherwise Azure blob lastModified, otherwise {@link Instant#EPOCH}.
     */
    private Instant effectiveInstant(BlobItem blob) {
        return parseTimestampFromName(blob.getName()).orElseGet(() -> {
            OffsetDateTime lm = blob.getProperties().getLastModified();
            return lm != null ? lm.toInstant() : Instant.EPOCH;
        });
    }

    /**
     * Attempts to parse the {@code yyyyMMdd'T'HHmmssZ} timestamp embedded in a backup blob name.
     *
     * <p>Example: {@code prod/postgres/barter-barter_db-20260701T194213Z.dump.gz}
     * → {@code 2026-07-01T19:42:13Z}
     */
    Optional<Instant> parseTimestampFromName(String blobName) {
        if (blobName == null) {
            return Optional.empty();
        }
        Matcher matcher = BLOB_TIMESTAMP_PATTERN.matcher(blobName);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String rawTs = matcher.group(1); // e.g. "20260701T194213Z"
        String withoutZ = rawTs.substring(0, rawTs.length() - 1); // strip trailing Z
        try {
            LocalDateTime ldt = LocalDateTime.parse(withoutZ, BLOB_TIMESTAMP_FORMATTER);
            return Optional.of(ldt.toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException ex) {
            log.debug(
                    "Could not parse backup timestamp from blob name segment '{}': {}",
                    rawTs, ex.getMessage());
            return Optional.empty();
        }
    }

    private AdminOperationsBackupsResponse placeholder() {
        return new AdminOperationsBackupsResponse()
                .availability(AVAILABILITY_PLACEHOLDER)
                .scheduledBackupEnabled(false)
                .note("Backup Azure configuration is not present in this environment. "
                        + "Set BACKUP_AZURE_CONNECTION_STRING, BACKUP_AZURE_CONTAINER, and "
                        + "BACKUP_AZURE_PREFIX to enable real backup visibility.");
    }

    private AdminOperationsBackupsResponse unavailable(String note) {
        return new AdminOperationsBackupsResponse()
                .availability(AVAILABILITY_UNAVAILABLE)
                .scheduledBackupEnabled(backupEnabled)
                .note(note);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Redacts AccountKey and SAS tokens from error messages before logging. */
    private static String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message
                .replaceAll("(?i)(AccountKey=)[^;\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)(SharedAccessSignature=)[^;\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)(sig=)[^&\\s]+", "$1[REDACTED]");
    }

    private static String rootCauseClass(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getClass().getName();
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage();
    }
}
