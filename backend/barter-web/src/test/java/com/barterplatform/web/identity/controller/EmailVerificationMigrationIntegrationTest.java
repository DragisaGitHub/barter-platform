package com.barterplatform.web.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class EmailVerificationMigrationIntegrationTest {

    @TempDir
    Path temporaryMigrationDirectory;

    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("barter_migration")
            .withUsername("barter_user")
            .withPassword("barter_password");

    @Test
    void v031MarksV030RetiredLegacyCodesUsedForOlderBackendRollback() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("29"))
                .load()
                .migrate();

        long userId;
        UUID legacyCodeUuid = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (PreparedStatement insertUser = connection.prepareStatement("""
                    insert into users (uuid, username, email, status, email_verified, mfa_enabled,
                                       created_at, preferred_language)
                    values (?, ?, ?, 'PENDING_VERIFICATION', false, false, ?, 'SR')
                    returning id
                    """)) {
                insertUser.setObject(1, UUID.randomUUID());
                insertUser.setString(2, "migration-user");
                insertUser.setString(3, "migration@example.com");
                insertUser.setObject(4, OffsetDateTime.now());
                try (ResultSet result = insertUser.executeQuery()) {
                    result.next();
                    userId = result.getLong(1);
                }
            }
            try (PreparedStatement insertCode = connection.prepareStatement("""
                    insert into email_verification_codes (uuid, user_id, code_hash, expires_at, created_at)
                    values (?, ?, 'legacy-plaintext-searchable-hash', ?, ?)
                    """)) {
                insertCode.setObject(1, legacyCodeUuid);
                insertCode.setLong(2, userId);
                insertCode.setObject(3, OffsetDateTime.now().plusMinutes(15));
                insertCode.setObject(4, OffsetDateTime.now());
                insertCode.executeUpdate();
            }
        }

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement query = connection.prepareStatement("""
                     select failed_attempts, used_at, invalidated_at,
                            used_at = invalidated_at as retirement_timestamp_matches,
                            counts_toward_issuance_limit
                     from email_verification_codes where uuid = ?
                     """)) {
            query.setObject(1, legacyCodeUuid);
            try (ResultSet result = query.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt("failed_attempts")).isZero();
                assertThat(result.getObject("used_at")).isNotNull();
                assertThat(result.getObject("invalidated_at")).isNotNull();
                assertThat(result.getBoolean("retirement_timestamp_matches")).isTrue();
                assertThat(result.getBoolean("counts_toward_issuance_limit")).isFalse();
            }

            // The previous backend selects outstanding codes with used_at IS NULL.
            try (PreparedStatement legacyLookup = connection.prepareStatement("""
                    select id from email_verification_codes
                    where user_id = ? and used_at is null
                    order by created_at desc limit 1
                    """)) {
                legacyLookup.setLong(1, userId);
                try (ResultSet result = legacyLookup.executeQuery()) {
                    assertThat(result.next()).isFalse();
                }
            }
        }

        Path currentMigrationDirectory = Path.of(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("db/migration")).toURI());
        Pattern migrationName = Pattern.compile("V(\\d+)_.*\\.sql");
        try (Stream<Path> migrations = Files.list(currentMigrationDirectory)) {
            for (Path migration : migrations.toList()) {
                Matcher match = migrationName.matcher(migration.getFileName().toString());
                if (match.matches() && Integer.parseInt(match.group(1)) < 30) {
                    Files.copy(migration, temporaryMigrationDirectory.resolve(migration.getFileName()));
                }
            }
        }

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + temporaryMigrationDirectory)
                .load()
                .validate();
    }
}
