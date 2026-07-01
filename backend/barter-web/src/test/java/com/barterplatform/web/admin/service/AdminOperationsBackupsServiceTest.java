package com.barterplatform.web.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.barterplatform.api.model.AdminOperationsBackupsResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link AdminOperationsBackupsService}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Placeholder state returned when Azure config is absent</li>
 *   <li>Filename timestamp parsing (happy path, edge cases, invalid input)</li>
 * </ul>
 *
 * <p>Real Azure Blob Storage calls are not tested here. Integration with Azure is
 * verified manually in staging / production environments.
 */
class AdminOperationsBackupsServiceTest {

    // ── Placeholder / missing config ─────────────────────────────────────────

    @Test
    void returnPlaceholderWhenConnectionStringAndContainerAreBlank() {
        AdminOperationsBackupsService service = serviceWith("", "", "", false);

        AdminOperationsBackupsResponse response = service.getBackups();

        assertThat(response.getAvailability()).isEqualTo("placeholder");
        assertThat(response.getScheduledBackupEnabled()).isFalse();
        assertThat(response.getNote()).isNotBlank();
        // No connection string, container, or blob metadata should appear in the response
        assertThat(response.getLastBackupTimestamp()).isNull();
    }

    @Test
    void returnPlaceholderWhenConnectionStringIsNullAndContainerIsBlank() {
        AdminOperationsBackupsService service = serviceWith(null, "", "prod/postgres", true);

        AdminOperationsBackupsResponse response = service.getBackups();

        assertThat(response.getAvailability()).isEqualTo("placeholder");
        assertThat(response.getScheduledBackupEnabled()).isFalse();
    }

    @Test
    void returnPlaceholderWhenConnectionStringProvidedButContainerIsBlank() {
        // Container is required; without it we cannot list blobs.
        AdminOperationsBackupsService service = serviceWith(
                "DefaultEndpointsProtocol=https;AccountName=x;AccountKey=key;EndpointSuffix=core.windows.net",
                "",
                "prod/postgres",
                true);

        AdminOperationsBackupsResponse response = service.getBackups();

        assertThat(response.getAvailability()).isEqualTo("placeholder");
    }

    // ── Filename timestamp parsing ────────────────────────────────────────────

    @Test
    void parsesTimestampFromValidBackupBlobName() {
        AdminOperationsBackupsService service = serviceWith("", "", "", false);

        Optional<Instant> result = service.parseTimestampFromName(
                "prod/postgres/barter-barter_db-20260701T194213Z.dump.gz");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(Instant.parse("2026-07-01T19:42:13Z"));
    }

    @Test
    void parsesTimestampFromBlobNameWithoutPathPrefix() {
        AdminOperationsBackupsService service = serviceWith("", "", "", false);

        Optional<Instant> result = service.parseTimestampFromName(
                "barter-barter_db-20250315T080000Z.dump.gz");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(Instant.parse("2025-03-15T08:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "barter-barter_db-20260701T194213Z.dump",        // missing .gz
            "barter-barter_db-20260701T1942Z.dump.gz",       // timestamp too short
            "barter-barter_db-2026-07-01T19:42:13Z.dump.gz", // ISO format (wrong)
            "barter-barter_db.dump.gz",                       // no timestamp segment
            "backup-no-timestamp.bin",                        // completely wrong
            ""                                                 // empty string
    })
    void returnsEmptyForUnparseableOrWrongExtension(String blobName) {
        AdminOperationsBackupsService service = serviceWith("", "", "", false);

        Optional<Instant> result = service.parseTimestampFromName(blobName);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyForNullBlobName() {
        AdminOperationsBackupsService service = serviceWith("", "", "", false);

        Optional<Instant> result = service.parseTimestampFromName(null);

        assertThat(result).isEmpty();
    }

    @Test
    void parsesTimestampAtMidnight() {
        AdminOperationsBackupsService service = serviceWith("", "", "", false);

        Optional<Instant> result = service.parseTimestampFromName(
                "prod/db/barter-barter_db-20260101T000000Z.dump.gz");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static AdminOperationsBackupsService serviceWith(
            String connectionString, String container, String prefix, boolean backupEnabled) {
        return new AdminOperationsBackupsService(connectionString, container, prefix, backupEnabled);
    }
}

