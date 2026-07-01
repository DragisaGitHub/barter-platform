package com.barterplatform.web.admin.service;

import com.barterplatform.api.model.AdminOperationsDeploymentsResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
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
        String version = applicationVersion();

        return new AdminOperationsDeploymentsResponse()
                .availability(lastDeploymentTimestamp != null ? AVAILABILITY_CONFIGURED : AVAILABILITY_PLACEHOLDER)
                .environment(String.join(",", activeProfiles()))
                .currentVersion(version)
                .lastDeploymentTimestamp(lastDeploymentTimestamp)
                .deploymentSource(null)
                .note(lastDeploymentTimestamp == null
                        ? "Deployment timestamp is not available. "
                                + "Set BARTER_DEPLOYED_AT or configure barter.deployment.state-file to enable tracking."
                        : null);
    }

    private OffsetDateTime resolveLastDeploymentTimestamp() {
        String value = firstNonBlank(
                environment.getProperty("barter.deployment.deployed-at"),
                environment.getProperty("BARTER_DEPLOYED_AT"));
        return parseDeploymentTimestamp(value);
    }

    private OffsetDateTime parseDeploymentTimestamp(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(normalized, DEPLOYMENT_STATE_TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private String applicationVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties != null && buildProperties.getVersion() != null && !buildProperties.getVersion().isBlank()) {
            return buildProperties.getVersion();
        }
        return AdminOperationsDeploymentsService.class.getPackage().getImplementationVersion();
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

