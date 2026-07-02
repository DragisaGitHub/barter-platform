package com.barterplatform.web.admin.service;

import com.barterplatform.api.model.AdminOperationsSecurityResponse;
import com.barterplatform.api.model.AdminSecurityAuthentication;
import com.barterplatform.api.model.AdminSecurityBackups;
import com.barterplatform.api.model.AdminSecurityCors;
import com.barterplatform.api.model.AdminSecurityDeploymentSafety;
import com.barterplatform.api.model.AdminSecurityEdge;
import com.barterplatform.api.model.AdminSecurityEmail;
import com.barterplatform.api.model.AdminSecurityObservability;
import com.barterplatform.api.model.AdminSecurityOverall;
import com.barterplatform.api.model.AdminSecurityStorage;
import com.barterplatform.web.security.SecurityProperties;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Provides a production-safe security posture snapshot for the Admin Operations Center.
 *
 * <p>Each section evaluates existing configuration and application state to produce a
 * status (OK / WARNING / CRITICAL / UNKNOWN) and optional notes. The service never
 * returns secrets, connection strings, credentials, or raw sensitive values. Values
 * that cannot be determined are returned as null or marked UNKNOWN.
 */
@Service
public class AdminOperationsSecurityService {

    private static final String STATUS_OK = "OK";
    private static final String STATUS_WARNING = "WARNING";
    private static final String STATUS_CRITICAL = "CRITICAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";

    private static final String DEFAULT_MAIL_FROM = "noreply@barter-platform.com";

    private final String jwtSecret;
    private final int accessTokenMinutes;
    private final int refreshTokenDays;
    private final boolean emailVerificationEnabled;
    private final boolean bootstrapAdminEnabled;
    private final String azureStorageConnectionString;
    private final String azureStorageContainer;
    private final String backupContainer;
    private final boolean backupEnabled;
    private final String backupPrefix;
    private final String mailHost;
    private final String mailFrom;
    private final String sentryDsn;
    private final Environment environment;
    private final SecurityProperties securityProperties;

    public AdminOperationsSecurityService(
            @Value("${barter.jwt.secret:}") String jwtSecret,
            @Value("${barter.jwt.access-token-expiration-minutes:15}") int accessTokenMinutes,
            @Value("${barter.jwt.refresh-token-expiration-days:7}") int refreshTokenDays,
            @Value("${barter.email-verification.enabled:true}") boolean emailVerificationEnabled,
            @Value("${barter.bootstrap.admin.enabled:false}") boolean bootstrapAdminEnabled,
            @Value("${barter.storage.azure.connection-string:}") String azureStorageConnectionString,
            @Value("${barter.storage.azure.container-name:}") String azureStorageContainer,
            @Value("${barter.backup.azure.container:}") String backupContainer,
            @Value("${barter.backup.enabled:false}") boolean backupEnabled,
            @Value("${barter.backup.azure.prefix:}") String backupPrefix,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${barter.mail.from:}") String mailFrom,
            @Value("${sentry.dsn:}") String sentryDsn,
            Environment environment,
            SecurityProperties securityProperties) {
        this.jwtSecret = jwtSecret;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
        this.emailVerificationEnabled = emailVerificationEnabled;
        this.bootstrapAdminEnabled = bootstrapAdminEnabled;
        this.azureStorageConnectionString = azureStorageConnectionString;
        this.azureStorageContainer = azureStorageContainer;
        this.backupContainer = backupContainer;
        this.backupEnabled = backupEnabled;
        this.backupPrefix = backupPrefix;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.sentryDsn = sentryDsn;
        this.environment = environment;
        this.securityProperties = securityProperties;
    }

    public AdminOperationsSecurityResponse getSecurity() {
        AdminSecurityAuthentication authentication = buildAuthentication();
        AdminSecurityCors cors = buildCors();
        AdminSecurityStorage storage = buildStorage();
        AdminSecurityBackups backups = buildBackups();
        AdminSecurityEmail email = buildEmail();
        AdminSecurityObservability observability = buildObservability();
        AdminSecurityDeploymentSafety deploymentSafety = buildDeploymentSafety();
        AdminSecurityEdge edge = buildEdge();

        String overallStatus = deriveOverallStatus(
                authentication.getAuthStatus(),
                cors.getCorsStatus(),
                storage.getStorageStatus(),
                backups.getBackupStatus(),
                email.getEmailStatus(),
                observability.getObservabilityStatus(),
                deploymentSafety.getDeploymentSafetyStatus(),
                edge.getSecurityHeadersStatus());

        List<String> overallNotes = collectCriticalAndWarningSummary(
                authentication.getAuthStatus(), "Authentication",
                cors.getCorsStatus(), "CORS",
                storage.getStorageStatus(), "Storage",
                backups.getBackupStatus(), "Backups",
                email.getEmailStatus(), "Email",
                observability.getObservabilityStatus(), "Observability",
                deploymentSafety.getDeploymentSafetyStatus(), "Deployment safety",
                edge.getSecurityHeadersStatus(), "Edge");

        AdminSecurityOverall overall = new AdminSecurityOverall()
                .overallStatus(overallStatus)
                .notes(overallNotes.isEmpty() ? null : overallNotes);

        return new AdminOperationsSecurityResponse()
                .authentication(authentication)
                .cors(cors)
                .storage(storage)
                .backups(backups)
                .email(email)
                .observability(observability)
                .deploymentSafety(deploymentSafety)
                .edge(edge)
                .overall(overall)
                .lastUpdated(OffsetDateTime.now(ZoneOffset.UTC));
    }

    // ── Section builders ──────────────────────────────────────────────────────

    private AdminSecurityAuthentication buildAuthentication() {
        boolean jwtConfigured = !isBlank(jwtSecret);
        List<String> notes = new ArrayList<>();
        String status;

        if (!jwtConfigured) {
            notes.add("JWT secret is not configured — authentication cannot function securely in production.");
            status = STATUS_CRITICAL;
        } else if (bootstrapAdminEnabled) {
            notes.add("Bootstrap admin is enabled — disable BARTER_BOOTSTRAP_ADMIN_ENABLED in production to prevent privilege escalation.");
            status = STATUS_CRITICAL;
        } else if (!emailVerificationEnabled) {
            notes.add("Email verification is disabled — unverified accounts can access the platform.");
            status = STATUS_CRITICAL;
        } else if (securityProperties.isSwaggerEnabled() && isProductionProfile()) {
            notes.add("Swagger UI is enabled in production — consider disabling BARTER_SWAGGER_ENABLED.");
            status = STATUS_CRITICAL;
        } else if (securityProperties.isSwaggerEnabled()) {
            notes.add("Swagger UI is enabled. Acceptable in non-production environments.");
            status = STATUS_WARNING;
        } else if (accessTokenMinutes > 60) {
            notes.add("Access token expiration exceeds 60 minutes — consider shorter-lived tokens for production.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityAuthentication()
                .jwtConfigured(jwtConfigured)
                .accessTokenMinutes(accessTokenMinutes)
                .refreshTokenDays(refreshTokenDays)
                .emailVerificationEnabled(emailVerificationEnabled)
                .bootstrapAdminEnabled(bootstrapAdminEnabled)
                .swaggerEnabled(securityProperties.isSwaggerEnabled())
                .authStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityCors buildCors() {
        List<String> allowedOrigins = securityProperties.getAllowedOrigins();
        boolean allowCredentials = securityProperties.isAllowCredentials();
        boolean originsConfigured = !allowedOrigins.isEmpty();
        List<String> notes = new ArrayList<>();
        String status;

        if (allowCredentials && !originsConfigured) {
            notes.add("allowCredentials=true with no allowed origins — this is a broadly permissive CORS configuration.");
            status = STATUS_CRITICAL;
        } else if (!originsConfigured) {
            notes.add("No allowed origins are configured — CORS may deny all cross-origin requests or default to open access.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityCors()
                .allowedOriginsConfigured(originsConfigured)
                .allowedOriginsCount(allowedOrigins.size())
                .allowCredentials(allowCredentials)
                .allowedMethods(securityProperties.getAllowedMethods())
                .exposedHeaders(securityProperties.getExposedHeaders())
                .corsStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityStorage buildStorage() {
        boolean azureBlobConfigured = !isBlank(azureStorageConnectionString);
        boolean imageContainerConfigured = !isBlank(azureStorageContainer);
        boolean backupContainerConfigured = !isBlank(backupContainer);
        List<String> notes = new ArrayList<>();
        String status;

        if (!azureBlobConfigured) {
            notes.add("Azure Blob Storage is not configured — item images use local disk storage.");
            status = STATUS_WARNING;
        } else if (!imageContainerConfigured) {
            notes.add("Azure Blob Storage is configured but the image container name is missing.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityStorage()
                .azureBlobConfigured(azureBlobConfigured)
                .imageContainerConfigured(imageContainerConfigured)
                .backupContainerConfigured(backupContainerConfigured)
                .storageStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityBackups buildBackups() {
        boolean backupContainerConfigured = !isBlank(backupContainer);
        boolean backupPrefixConfigured = !isBlank(backupPrefix);
        String backupMode = backupContainerConfigured ? "azure-blob" : "unconfigured";
        List<String> notes = new ArrayList<>();
        String status;

        if (!backupEnabled) {
            notes.add("Scheduled backups are disabled (BACKUP_ENABLED=false). Enable database backups in production.");
            status = STATUS_CRITICAL;
        } else if (!backupContainerConfigured) {
            notes.add("Backups are enabled but the Azure backup container is not configured.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityBackups()
                .backupEnabled(backupEnabled)
                .backupMode(backupMode)
                .backupContainerConfigured(backupContainerConfigured)
                .backupPrefixConfigured(backupPrefixConfigured)
                .backupStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityEmail buildEmail() {
        boolean smtpConfigured = !isBlank(mailHost);
        boolean mailFromConfigured = !isBlank(mailFrom)
                && !mailFrom.trim().equalsIgnoreCase(DEFAULT_MAIL_FROM);
        List<String> notes = new ArrayList<>();
        String status;

        if (!emailVerificationEnabled) {
            notes.add("Email verification is disabled — all new accounts bypass email confirmation.");
            status = STATUS_CRITICAL;
        } else if (!smtpConfigured) {
            notes.add("SMTP host is not configured — email delivery falls back to console logging only.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityEmail()
                .smtpConfigured(smtpConfigured)
                .mailFromConfigured(mailFromConfigured)
                .emailVerificationEnabled(emailVerificationEnabled)
                .emailStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityObservability buildObservability() {
        boolean sentryConfigured = !isBlank(sentryDsn);
        List<String> notes = new ArrayList<>();
        String status;

        if (!sentryConfigured) {
            notes.add("Backend Sentry DSN is not configured — exceptions are not tracked externally.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityObservability()
                .backendSentryConfigured(sentryConfigured)
                .frontendSentryRuntimeKnown(null) // frontend state is not observable from backend
                .operationsMonitoringAvailable(true)
                .observabilityStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityDeploymentSafety buildDeploymentSafety() {
        String releaseVersion = firstNonBlank(
                environment.getProperty("barter.deployment.release-version"),
                environment.getProperty("BARTER_RELEASE_VERSION"));
        String deployedAt = firstNonBlank(
                environment.getProperty("barter.deployment.deployed-at"),
                environment.getProperty("BARTER_DEPLOYED_AT"));
        String deploySource = firstNonBlank(
                environment.getProperty("barter.deployment.deploy-source"),
                environment.getProperty("BARTER_DEPLOY_SOURCE"));

        boolean releaseVersionConfigured = releaseVersion != null;
        boolean deployedAtConfigured = deployedAt != null;
        boolean deploySourceConfigured = deploySource != null;

        Boolean immutableImageTagsDetected = null;
        if (releaseVersion != null) {
            boolean isMutableTag = releaseVersion.equalsIgnoreCase("latest")
                    || releaseVersion.equalsIgnoreCase("main")
                    || releaseVersion.equalsIgnoreCase("master")
                    || releaseVersion.equalsIgnoreCase("develop");
            immutableImageTagsDetected = !isMutableTag;
        }

        List<String> notes = new ArrayList<>();
        String status;

        if (!releaseVersionConfigured && !deployedAtConfigured) {
            notes.add("Release version and deployment timestamp are not set — deployment tracking is unavailable.");
            status = STATUS_WARNING;
        } else if (Boolean.FALSE.equals(immutableImageTagsDetected)) {
            notes.add("Release version appears to be a mutable tag (latest/main/master/develop). Use immutable image tags in production.");
            status = STATUS_WARNING;
        } else if (!deploySourceConfigured) {
            notes.add("Deployment source is not configured — BARTER_DEPLOY_SOURCE is recommended for audit trails.");
            status = STATUS_WARNING;
        } else {
            status = STATUS_OK;
        }

        return new AdminSecurityDeploymentSafety()
                .releaseVersionConfigured(releaseVersionConfigured)
                .deployedAtConfigured(deployedAtConfigured)
                .deploySourceConfigured(deploySourceConfigured)
                .immutableImageTagsDetected(immutableImageTagsDetected)
                .deploymentSafetyStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    private AdminSecurityEdge buildEdge() {
        // HSTS is always configured in SecurityConfig — this is a known fact about the codebase.
        boolean hstsKnown = true;
        boolean isProd = isProductionProfile();

        // HTTPS and Caddy are assumed in production (Caddy handles TLS termination).
        // In non-production environments we cannot determine this from the backend alone.
        Boolean httpsAssumedEnabled = isProd ? Boolean.TRUE : null;
        Boolean caddyConfigured = isProd ? Boolean.TRUE : null;

        List<String> notes = new ArrayList<>();
        String status;

        if (isProd) {
            status = STATUS_OK;
            notes.add("HSTS, Content-Security-Policy, X-Frame-Options, and Referrer-Policy headers are enforced by Spring Security. Caddy is assumed as the HTTPS termination layer in production.");
        } else {
            status = STATUS_UNKNOWN;
            notes.add("Edge security state cannot be fully determined in non-production environments. HSTS and security headers are active at the application layer.");
        }

        return new AdminSecurityEdge()
                .httpsAssumedEnabled(httpsAssumedEnabled)
                .caddyConfigured(caddyConfigured)
                .hstsKnown(hstsKnown)
                .securityHeadersStatus(status)
                .notes(notes.isEmpty() ? null : notes);
    }

    // ── Status helpers ────────────────────────────────────────────────────────

    /**
     * Derives the overall status as the worst severity across all section statuses.
     * Priority: CRITICAL > WARNING > UNKNOWN > OK.
     */
    private String deriveOverallStatus(String... statuses) {
        boolean hasCritical = false;
        boolean hasWarning = false;
        boolean hasUnknown = false;
        for (String s : statuses) {
            if (STATUS_CRITICAL.equals(s)) hasCritical = true;
            else if (STATUS_WARNING.equals(s)) hasWarning = true;
            else if (STATUS_UNKNOWN.equals(s)) hasUnknown = true;
        }
        if (hasCritical) return STATUS_CRITICAL;
        if (hasWarning) return STATUS_WARNING;
        if (hasUnknown) return STATUS_UNKNOWN;
        return STATUS_OK;
    }

    /**
     * Collects human-readable summary notes for sections that are in WARNING or CRITICAL state.
     * Arguments are alternating status/name pairs.
     */
    private List<String> collectCriticalAndWarningSummary(String... sectionStatusAndNames) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < sectionStatusAndNames.length - 1; i += 2) {
            String statusValue = sectionStatusAndNames[i];
            String name = sectionStatusAndNames[i + 1];
            if (STATUS_CRITICAL.equals(statusValue)) {
                result.add(name + " requires immediate attention (CRITICAL).");
            } else if (STATUS_WARNING.equals(statusValue)) {
                result.add(name + " has a non-critical configuration warning.");
            }
        }
        return result;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private boolean isProductionProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }
}

