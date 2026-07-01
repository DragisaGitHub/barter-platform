package com.barterplatform.web.admin.service;

import com.barterplatform.api.model.AdminOperationsDeploymentsResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Provides deployment status information for the Admin Operations Center.
 *
 * <p>Reads safe environment metadata available to the application — active Spring profiles,
 * build version, and the last deployment timestamp — without exposing secrets, raw
 * environment variables, or connection strings. Real GitHub Actions integration is deferred.
 */
@Service
public class AdminOperationsDeploymentsService {

    private static final String DEFAULT_PROFILE = "default";
    private static final String AVAILABILITY_PLACEHOLDER = "placeholder";
    private static final String AVAILABILITY_CONFIGURED = "configured";
    private static final DateTimeFormatter DEPLOYMENT_STATE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public AdminOperationsDeploymentsService(
            Environment environment,
            ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.environment = environment;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    public AdminOperationsDeploymentsResponse getDeployments() {
        OffsetDateTime lastDeploymentTimestamp = resolveLastDeploymentTimestamp();
        String releaseVersion = resolveReleaseVersion();
        String deploymentSource = resolveDeploymentSource();
        boolean hasDeployInfo = releaseVersion != null || lastDeploymentTimestamp != null;

        return new AdminOperationsDeploymentsResponse()
                .availability(hasDeployInfo ? AVAILABILITY_CONFIGURED : AVAILABILITY_PLACEHOLDER)
                .environment(String.join(",", activeProfiles()))
                .releaseVersion(releaseVersion)
                .currentVersion(buildVersion())
                .lastDeploymentTimestamp(lastDeploymentTimestamp)
                .deploymentSource(deploymentSource)
                .note(hasDeployInfo ? null
                        : "Deployment info is not available. "
                                + "Set BARTER_RELEASE_VERSION, BARTER_DEPLOYED_AT, and BARTER_DEPLOY_SOURCE "
                                + "in the environment to enable deployment tracking.");
    }

    private OffsetDateTime resolveLastDeploymentTimestamp() {
        String value = firstNonBlank(
                environment.getProperty("barter.deployment.deployed-at"),
                environment.getProperty("BARTER_DEPLOYED_AT"));
        return parseDeploymentTimestamp(value);
    }

    private String resolveReleaseVersion() {
        return firstNonBlank(
                environment.getProperty("barter.deployment.release-version"),
                environment.getProperty("BARTER_RELEASE_VERSION"));
    }

    private String resolveDeploymentSource() {
        return firstNonBlank(
                environment.getProperty("barter.deployment.deploy-source"),
                environment.getProperty("BARTER_DEPLOY_SOURCE"));
    }

    private String buildVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties != null && buildProperties.getVersion() != null && !buildProperties.getVersion().isBlank()) {
            return buildProperties.getVersion();
        }
        return AdminOperationsDeploymentsService.class.getPackage().getImplementationVersion();
    }

    private OffsetDateTime parseDeploymentTimestamp(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return getOffsetDateTime(normalized, DEPLOYMENT_STATE_TIMESTAMP_FORMATTER);
    }

    @Nullable
    static OffsetDateTime getOffsetDateTime(String normalized, DateTimeFormatter deploymentStateTimestampFormatter) {
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(normalized, deploymentStateTimestampFormatter).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }


    private List<String> activeProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return List.of(DEFAULT_PROFILE);
        }
        return Arrays.asList(activeProfiles);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}

