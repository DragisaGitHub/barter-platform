package com.barterplatform.web.admin.service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import com.barterplatform.api.model.AdminMonitoringApplication;
import com.barterplatform.api.model.AdminMonitoringDatabase;
import com.barterplatform.api.model.AdminMonitoringHttp;
import com.barterplatform.api.model.AdminMonitoringJvm;
import com.barterplatform.api.model.AdminMonitoringMemory;
import com.barterplatform.api.model.AdminMonitoringPlatformHealth;
import com.barterplatform.api.model.AdminMonitoringStorage;
import com.barterplatform.api.model.AdminOperationsMonitoringResponse;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Provides a real-time operational monitoring snapshot for the Admin Operations Center.
 *
 * <p>All metrics are collected from live sources:
 * <ul>
 *   <li>Spring Boot Actuator {@code ApplicationAvailability} for liveness and readiness state</li>
 *   <li>{@code BuildProperties} for build version</li>
 *   <li>{@link java.lang.management.ManagementFactory} MXBeans for JVM heap, threads, and uptime</li>
 *   <li>{@code com.sun.management.OperatingSystemMXBean} for system-level memory (gracefully degraded)</li>
 *   <li>HikariCP {@code HikariPoolMXBean} for database connection pool stats (gracefully degraded)</li>
 *   <li>Azure Blob Storage SDK for storage reachability (gracefully degraded when not configured)</li>
 * </ul>
 *
 * <p>Metrics that cannot be determined are returned as {@code null} with explanatory notes.
 * Connection strings and secrets are never logged or included in any response.
 */
@Service
public class AdminOperationsMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(AdminOperationsMonitoringService.class);

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_NOT_PROBED = "NOT_PROBED";
    private static final String STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";
    private static final String DEFAULT_PROFILE = "default";

    /**
     * Single attempt, 10-second timeout — monitoring is a read-only admin probe;
     * slow Azure calls must not block the endpoint beyond a reasonable threshold.
     */
    private static final RequestRetryOptions BLOB_RETRY_OPTIONS =
            new RequestRetryOptions(RetryPolicyType.EXPONENTIAL, 1, 10, null, null, null);

    private final DataSource dataSource;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final ApplicationAvailability applicationAvailability;
    private final String storageProviderType;
    private final String backupConnectionString;
    private final String backupContainer;

    public AdminOperationsMonitoringService(
            DataSource dataSource,
            Environment environment,
            ObjectProvider<BuildProperties> buildPropertiesProvider,
            ApplicationAvailability applicationAvailability,
            @Value("${barter.storage.type:local}") String storageProviderType,
            @Value("${barter.backup.azure.connection-string:}") String backupConnectionString,
            @Value("${barter.backup.azure.container:}") String backupContainer) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.buildPropertiesProvider = buildPropertiesProvider;
        this.applicationAvailability = applicationAvailability;
        this.storageProviderType = normalizeStorageProvider(storageProviderType);
        this.backupConnectionString = backupConnectionString;
        this.backupContainer = backupContainer;
    }

    public AdminOperationsMonitoringResponse getMonitoring() {
        boolean dbValid = probeDatabaseValid();
        String dbStatus = dbValid ? STATUS_UP : STATUS_DOWN;
        BlobProbeResult blobResult = probeBlobStorage();

        ReadinessState readiness = applicationAvailability.getReadinessState();
        LivenessState liveness = applicationAvailability.getLivenessState();

        String overallStatus = computeOverallStatus(dbValid, liveness, readiness);

        return new AdminOperationsMonitoringResponse()
                .platform(platformHealth(dbStatus, blobResult))
                .application(application())
                .jvm(jvm())
                .memory(memory())
                .database(database(dbValid))
                .http(http(readiness, liveness))
                .storage(storage(blobResult))
                .overallStatus(overallStatus)
                .lastUpdated(OffsetDateTime.now(ZoneOffset.UTC))
                .notes(collectNotes(blobResult));
    }

    // ── Platform health ──────────────────────────────────────────────────────

    private AdminMonitoringPlatformHealth platformHealth(String dbStatus, BlobProbeResult blobResult) {
        return new AdminMonitoringPlatformHealth()
                .backendStatus(STATUS_UP)
                .frontendStatus(STATUS_NOT_PROBED)
                .landingStatus(STATUS_NOT_PROBED)
                .databaseStatus(dbStatus)
                .blobStorageStatus(blobResult.serviceStatus());
    }

    // ── Application ──────────────────────────────────────────────────────────

    private AdminMonitoringApplication application() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtime.getUptime();

        return new AdminMonitoringApplication()
                .currentRelease(resolveReleaseVersion())
                .buildVersion(buildVersion())
                .activeProfiles(activeProfiles())
                .javaVersion(System.getProperty("java.version", "unknown"))
                .springBootVersion(springBootVersion())
                .uptimeSeconds(uptimeMs >= 0 ? uptimeMs / 1_000 : null)
                .serverTime(OffsetDateTime.now(ZoneOffset.UTC));
    }

    // ── JVM ──────────────────────────────────────────────────────────────────

    private AdminMonitoringJvm jvm() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();

        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memory.getNonHeapMemoryUsage().getUsed();

        return new AdminMonitoringJvm()
                .heapUsedBytes(heapUsed >= 0 ? heapUsed : null)
                .heapMaxBytes(heapMax > 0 ? heapMax : null)
                .nonHeapUsedBytes(nonHeapUsed >= 0 ? nonHeapUsed : null)
                .threadCount(threads.getThreadCount())
                .daemonThreadCount(threads.getDaemonThreadCount())
                .processors(Runtime.getRuntime().availableProcessors());
    }

    // ── Memory ───────────────────────────────────────────────────────────────

    private AdminMonitoringMemory memory() {
        try {
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                long total = sunOs.getTotalMemorySize();
                long free = sunOs.getFreeMemorySize();
                long used = total >= 0 && free >= 0 ? total - free : -1;
                return new AdminMonitoringMemory()
                        .systemMemoryTotalBytes(total > 0 ? total : null)
                        .systemMemoryUsedBytes(used > 0 ? used : null)
                        .systemMemoryAvailableBytes(free >= 0 ? free : null);
            }
        } catch (Exception ex) {
            log.debug("System memory information unavailable", ex);
        }
        return new AdminMonitoringMemory()
                .note("System memory statistics are unavailable on this JVM or operating system.");
    }

    // ── Database ─────────────────────────────────────────────────────────────

    private AdminMonitoringDatabase database(boolean datasourceValid) {
        AdminMonitoringDatabase db = new AdminMonitoringDatabase().datasourceValid(datasourceValid);

        if (dataSource instanceof HikariDataSource hikari) {
            try {
                HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                if (pool != null) {
                    db.activeConnections(pool.getActiveConnections())
                            .idleConnections(pool.getIdleConnections())
                            .maxPoolSize(hikari.getMaximumPoolSize());
                }
            } catch (Exception ex) {
                log.debug("HikariCP pool statistics unavailable", ex);
                db.note("Connection pool statistics could not be read from HikariCP.");
            }
        } else {
            db.note("Connection pool statistics are only available for HikariCP datasources.");
        }

        return db;
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private AdminMonitoringHttp http(ReadinessState readiness, LivenessState liveness) {
        return new AdminMonitoringHttp()
                .readinessStatus(readiness.name())
                .livenessStatus(liveness.name());
    }

    // ── Storage ──────────────────────────────────────────────────────────────

    private AdminMonitoringStorage storage(BlobProbeResult blobResult) {
        AdminMonitoringStorage storage = new AdminMonitoringStorage()
                .storageProviderType(storageProviderType)
                .blobStorageReachable(blobResult.serviceReachable())
                .backupContainerReachable(blobResult.containerReachable());

        if (blobResult.note() != null) {
            storage.note(blobResult.note());
        }
        return storage;
    }

    // ── Azure Blob probe ─────────────────────────────────────────────────────

    private BlobProbeResult probeBlobStorage() {
        if (backupConnectionString == null || backupConnectionString.isBlank()) {
            if ("azure".equals(storageProviderType)) {
                return BlobProbeResult.notConfigured(
                        "Azure Blob Storage is the configured provider but no backup connection string is available for probing.");
            }
            return BlobProbeResult.notConfigured(null);
        }

        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(backupConnectionString)
                    .retryOptions(BLOB_RETRY_OPTIONS)
                    .buildClient();

            // Lightweight probe: get account properties to verify connectivity
            serviceClient.getProperties();
            boolean serviceReachable = true;

            // Check backup container exists
            Boolean containerReachable = null;
            if (backupContainer != null && !backupContainer.isBlank()) {
                try {
                    BlobContainerClient containerClient = serviceClient.getBlobContainerClient(backupContainer);
                    containerReachable = containerClient.exists();
                } catch (Exception ex) {
                    log.warn("Backup container existence check failed: {}", ex.getMessage());
                    containerReachable = false;
                }
            }

            return new BlobProbeResult(STATUS_UP, serviceReachable, containerReachable, null);
        } catch (Exception ex) {
            log.warn("Azure Blob Storage probe failed: {}", ex.getMessage());
            return new BlobProbeResult(STATUS_DOWN, false, null,
                    "Azure Blob Storage probe failed. See application logs for details.");
        }
    }

    // ── Overall status ───────────────────────────────────────────────────────

    private String computeOverallStatus(boolean dbValid, LivenessState liveness, ReadinessState readiness) {
        if (!dbValid || LivenessState.BROKEN.equals(liveness)) {
            return STATUS_DOWN;
        }
        if (ReadinessState.REFUSING_TRAFFIC.equals(readiness)) {
            return STATUS_DEGRADED;
        }
        return STATUS_UP;
    }

    // ── Notes ────────────────────────────────────────────────────────────────

    private List<String> collectNotes(BlobProbeResult blobResult) {
        List<String> notes = new ArrayList<>();
        notes.add("Frontend and landing services are not probed from the backend process.");
        if (blobResult.note() != null) {
            notes.add(blobResult.note());
        }
        return notes.isEmpty() ? null : notes;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean probeDatabaseValid() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(1);
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolveReleaseVersion() {
        String fromProp = environment.getProperty("barter.deployment.release-version");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp.trim();
        }
        String fromEnv = environment.getProperty("BARTER_RELEASE_VERSION");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return null;
    }

    private String buildVersion() {
        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        if (bp != null && bp.getVersion() != null && !bp.getVersion().isBlank()) {
            return bp.getVersion();
        }
        return AdminOperationsMonitoringService.class.getPackage().getImplementationVersion();
    }

    private String springBootVersion() {
        try {
            return SpringBootVersion.getVersion();
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return List.of(DEFAULT_PROFILE);
        }
        return Arrays.asList(profiles);
    }

    private String normalizeStorageProvider(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }

    // ── Inner record ─────────────────────────────────────────────────────────

    /**
     * Immutable result of a single Azure Blob Storage probe attempt.
     *
     * @param serviceStatus    String status for the platform health section (UP / DOWN / NOT_CONFIGURED).
     * @param serviceReachable Boolean reachability flag for the blob storage response, or null.
     * @param containerReachable Boolean reachability flag for the backup container, or null.
     * @param note             Explanatory note, or null.
     */
    record BlobProbeResult(
            String serviceStatus,
            Boolean serviceReachable,
            Boolean containerReachable,
            String note) {

        static BlobProbeResult notConfigured(String note) {
            return new BlobProbeResult(STATUS_NOT_CONFIGURED, null, null, note);
        }
    }
}

