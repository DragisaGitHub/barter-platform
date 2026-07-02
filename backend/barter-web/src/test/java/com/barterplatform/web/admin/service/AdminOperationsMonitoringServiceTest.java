package com.barterplatform.web.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

class AdminOperationsMonitoringServiceTest {

    private DataSource dataSource;
    private Connection connection;
    private Environment environment;
    private ObjectProvider<BuildProperties> buildPropertiesProvider;
    private ApplicationAvailability applicationAvailability;
    private AdminOperationsMonitoringService service;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        environment = mock(Environment.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        buildPropertiesProvider = provider;
        applicationAvailability = mock(ApplicationAvailability.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(environment.getProperty("barter.deployment.release-version")).thenReturn(null);
        when(environment.getProperty("BARTER_RELEASE_VERSION")).thenReturn(null);
        when(buildPropertiesProvider.getIfAvailable()).thenReturn(null);
        when(applicationAvailability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
        when(applicationAvailability.getLivenessState()).thenReturn(LivenessState.CORRECT);

        service = createService("", "");
    }

    private AdminOperationsMonitoringService createService(String backupConnectionString, String backupContainer) {
        return new AdminOperationsMonitoringService(
                dataSource,
                environment,
                buildPropertiesProvider,
                applicationAvailability,
                "local",
                backupConnectionString,
                backupContainer);
    }

    @Test
    void getMonitoringReturnsUpStatusWhenAllHealthy() {
        var result = service.getMonitoring();

        assertThat(result.getOverallStatus()).isEqualTo("UP");
        assertThat(result.getPlatform().getBackendStatus()).isEqualTo("UP");
        assertThat(result.getPlatform().getDatabaseStatus()).isEqualTo("UP");
        assertThat(result.getHttp().getLivenessStatus()).isEqualTo("CORRECT");
        assertThat(result.getHttp().getReadinessStatus()).isEqualTo("ACCEPTING_TRAFFIC");
        assertThat(result.getLastUpdated()).isNotNull();
    }

    @Test
    void getMonitoringReturnsDegradedWhenDatabaseDown() throws Exception {
        when(connection.isValid(1)).thenReturn(false);

        var result = service.getMonitoring();

        assertThat(result.getOverallStatus()).isEqualTo("DOWN");
        assertThat(result.getPlatform().getDatabaseStatus()).isEqualTo("DOWN");
        assertThat(result.getDatabase().getDatasourceValid()).isFalse();
    }

    @Test
    void getMonitoringReturnsDegradedWhenLivenessBroken() {
        when(applicationAvailability.getLivenessState()).thenReturn(LivenessState.BROKEN);

        var result = service.getMonitoring();

        assertThat(result.getOverallStatus()).isEqualTo("DOWN");
        assertThat(result.getHttp().getLivenessStatus()).isEqualTo("BROKEN");
    }

    @Test
    void getMonitoringReturnsDegradedWhenRefusingTraffic() {
        when(applicationAvailability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);

        var result = service.getMonitoring();

        assertThat(result.getOverallStatus()).isEqualTo("DEGRADED");
        assertThat(result.getHttp().getReadinessStatus()).isEqualTo("REFUSING_TRAFFIC");
    }

    @Test
    void getMonitoringPopulatesJvmMetrics() {
        var result = service.getMonitoring();

        assertThat(result.getJvm().getProcessors()).isGreaterThan(0);
        // heap used should be positive
        assertThat(result.getJvm().getHeapUsedBytes()).isGreaterThan(0L);
        // thread count should be positive
        assertThat(result.getJvm().getThreadCount()).isGreaterThan(0);
    }

    @Test
    void getMonitoringPopulatesApplicationMetrics() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        var result = service.getMonitoring();

        assertThat(result.getApplication().getActiveProfiles()).containsExactly("prod");
        assertThat(result.getApplication().getJavaVersion()).isNotBlank();
        assertThat(result.getApplication().getServerTime()).isNotNull();
    }

    @Test
    void getMonitoringUsesDefaultProfileWhenNoProfilesActive() {
        when(environment.getActiveProfiles()).thenReturn(new String[0]);

        var result = service.getMonitoring();

        assertThat(result.getApplication().getActiveProfiles()).containsExactly("default");
    }

    @Test
    void getMonitoringPopulatesReleaseVersionFromEnvironment() {
        when(environment.getProperty("barter.deployment.release-version")).thenReturn("v1.2.3");

        var result = service.getMonitoring();

        assertThat(result.getApplication().getCurrentRelease()).isEqualTo("v1.2.3");
    }

    @Test
    void getMonitoringReturnsNullReleaseVersionWhenNotConfigured() {
        var result = service.getMonitoring();

        assertThat(result.getApplication().getCurrentRelease()).isNull();
    }

    @Test
    void getMonitoringSetsFrontendAndLandingStatusAsNotProbed() {
        var result = service.getMonitoring();

        assertThat(result.getPlatform().getFrontendStatus()).isEqualTo("NOT_PROBED");
        assertThat(result.getPlatform().getLandingStatus()).isEqualTo("NOT_PROBED");
    }

    @Test
    void getMonitoringSetsBlobStorageNotConfiguredWhenNoConnectionString() {
        var result = service.getMonitoring();

        assertThat(result.getPlatform().getBlobStorageStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(result.getStorage().getBlobStorageReachable()).isNull();
        assertThat(result.getStorage().getBackupContainerReachable()).isNull();
    }

    @Test
    void getMonitoringReturnsDatabaseValidFalseWhenConnectionThrows() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        var result = service.getMonitoring();

        assertThat(result.getDatabase().getDatasourceValid()).isFalse();
        assertThat(result.getPlatform().getDatabaseStatus()).isEqualTo("DOWN");
    }

    @Test
    void getMonitoringAlwaysIncludesNotesAboutUnprobedServices() {
        var result = service.getMonitoring();

        assertThat(result.getNotes()).isNotNull();
        assertThat(result.getNotes()).anyMatch(note -> note.contains("Frontend") || note.contains("frontend"));
    }

    @Test
    void getMonitoringReturnsStorageProviderType() {
        var result = service.getMonitoring();

        assertThat(result.getStorage().getStorageProviderType()).isEqualTo("local");
    }

    @Test
    void getMonitoringUptimeIsNonNegative() {
        var result = service.getMonitoring();

        if (result.getApplication().getUptimeSeconds() != null) {
            assertThat(result.getApplication().getUptimeSeconds()).isGreaterThanOrEqualTo(0L);
        }
    }
}

