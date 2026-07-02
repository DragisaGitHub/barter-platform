package com.barterplatform.web.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.barterplatform.api.model.AdminOperationsCostsResponse;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link AdminOperationsCostsService}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Placeholder state returned when Azure configuration is absent</li>
 *   <li>UsageDate integer → ISO date string conversion</li>
 *   <li>Response does not contain secrets</li>
 *   <li>Request builders use {@code Custom} timeframe with explicit {@code timePeriod}</li>
 *   <li>Negative cache TTL constant is set</li>
 * </ul>
 *
 * <p>Real Azure Cost Management API calls are not tested here.
 * Integration with Azure is verified in staging/production environments.
 */
class AdminOperationsCostsServiceTest {

    // ── Placeholder / missing config ─────────────────────────────────────────

    @Test
    void returnPlaceholderWhenAllConfigIsBlank() {
        AdminOperationsCostsService service = serviceWith("", "", "", "");

        AdminOperationsCostsResponse response = service.getCosts();

        assertThat(response.getAvailability()).isEqualTo("placeholder");
        assertThat(response.getNote()).isNotBlank();
        // No cost data or credentials should appear in the response
        assertThat(response.getCurrentMonthCost()).isNull();
        assertThat(response.getPreviousMonthCost()).isNull();
        assertThat(response.getDailyTrend()).isNull();
        assertThat(response.getServiceBreakdown()).isNull();
    }

    @Test
    void returnPlaceholderWhenTenantIdMissing() {
        AdminOperationsCostsService service = serviceWith("", "client-id", "secret", "sub-id");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    @Test
    void returnPlaceholderWhenClientIdMissing() {
        AdminOperationsCostsService service = serviceWith("tenant", "", "secret", "sub-id");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    @Test
    void returnPlaceholderWhenClientSecretMissing() {
        AdminOperationsCostsService service = serviceWith("tenant", "client-id", "", "sub-id");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    @Test
    void returnPlaceholderWhenSubscriptionIdAndScopeBothMissing() {
        AdminOperationsCostsService service = serviceWith("tenant", "client-id", "secret", "");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    // ── No secrets in placeholder response ──────────────────────────────────

    @Test
    void placeholderResponseDoesNotContainSecrets() {
        // Scope/subscriptionId is blank → service returns placeholder immediately
        // without making any network calls. The note must not expose credentials.
        AdminOperationsCostsResponse response =
                serviceWith("my-tenant", "my-client", "super-secret", "").getCosts();

        // Placeholder because scope is unresolvable
        assertThat(response.getAvailability()).isEqualTo("placeholder");
        String note = response.getNote() != null ? response.getNote() : "";
        assertThat(note).doesNotContain("my-tenant");
        assertThat(note).doesNotContain("my-client");
        assertThat(note).doesNotContain("super-secret");
    }

    // ── UsageDate integer parsing ────────────────────────────────────────────

    @Test
    void formatsValidUsageDateInteger() {
        assertThat(AdminOperationsCostsService.formatUsageDate(20260701L)).isEqualTo("2026-07-01");
    }

    @Test
    void formatsUsageDateAtStartOfYear() {
        assertThat(AdminOperationsCostsService.formatUsageDate(20260101L)).isEqualTo("2026-01-01");
    }

    @Test
    void formatsUsageDateAtEndOfYear() {
        assertThat(AdminOperationsCostsService.formatUsageDate(20261231L)).isEqualTo("2026-12-31");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, 123L, 2026070L /* too short */, 202607011L /* too long */})
    void returnsNullForInvalidUsageDateIntegers(long usageDateInt) {
        assertThat(AdminOperationsCostsService.formatUsageDate(usageDateInt)).isNull();
    }

    // ── Request builders – Custom timeframe with explicit timePeriod ─────────

    @Test
    void currentMonthRequestUsesCustomTimeframeWithExplicitTimePeriod() {
        LocalDate today = LocalDate.of(2026, 7, 2);

        Map<String, Object> request = AdminOperationsCostsService.buildCurrentMonthRequest(today);

        assertThat(request).containsEntry("timeframe", "Custom");
        assertThat(request).containsKey("timePeriod");
        @SuppressWarnings("unchecked")
        Map<String, Object> timePeriod = (Map<String, Object>) request.get("timePeriod");
        assertThat(timePeriod).containsEntry("from", "2026-07-01");
        assertThat(timePeriod).containsEntry("to", "2026-07-03"); // today + 1 day
    }

    @Test
    void currentMonthRequestAtMonthStartUsesCorrectTimePeriod() {
        LocalDate firstOfMonth = LocalDate.of(2026, 3, 1);

        Map<String, Object> request = AdminOperationsCostsService.buildCurrentMonthRequest(firstOfMonth);

        @SuppressWarnings("unchecked")
        Map<String, Object> timePeriod = (Map<String, Object>) request.get("timePeriod");
        assertThat(timePeriod).containsEntry("from", "2026-03-01");
        assertThat(timePeriod).containsEntry("to", "2026-03-02");
    }

    @Test
    void previousMonthRequestUsesCustomTimeframeWithExplicitTimePeriod() {
        LocalDate today = LocalDate.of(2026, 7, 2);

        Map<String, Object> request = AdminOperationsCostsService.buildPreviousMonthRequest(today);

        assertThat(request).containsEntry("timeframe", "Custom");
        assertThat(request).containsKey("timePeriod");
        @SuppressWarnings("unchecked")
        Map<String, Object> timePeriod = (Map<String, Object>) request.get("timePeriod");
        assertThat(timePeriod).containsEntry("from", "2026-06-01");
        assertThat(timePeriod).containsEntry("to", "2026-07-01"); // exclusive: first day of current month
    }

    @Test
    void previousMonthRequestHandlesJanuaryCorrectly() {
        // Previous month wraps to December of the prior year
        LocalDate today = LocalDate.of(2026, 1, 15);

        Map<String, Object> request = AdminOperationsCostsService.buildPreviousMonthRequest(today);

        @SuppressWarnings("unchecked")
        Map<String, Object> timePeriod = (Map<String, Object>) request.get("timePeriod");
        assertThat(timePeriod).containsEntry("from", "2025-12-01");
        assertThat(timePeriod).containsEntry("to", "2026-01-01");
    }

    @Test
    void requestBuildersNeverUseDeprecatedTimeframeValues() {
        LocalDate today = LocalDate.of(2026, 7, 2);

        Map<String, Object> current = AdminOperationsCostsService.buildCurrentMonthRequest(today);
        Map<String, Object> previous = AdminOperationsCostsService.buildPreviousMonthRequest(today);

        // Neither builder should use the timeframe values that cause Azure 400 errors
        assertThat(current.values()).doesNotContain("TheLastMonth", "MonthToDate");
        assertThat(previous.values()).doesNotContain("TheLastMonth", "MonthToDate");
    }

    // ── Negative cache TTL ───────────────────────────────────────────────────

    @Test
    void errorCacheTtlIsConfigured() {
        assertThat(AdminOperationsCostsService.ERROR_CACHE_TTL_MINUTES).isEqualTo(5);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static AdminOperationsCostsService serviceWith(
            String tenantId, String clientId, String clientSecret,
            String subscriptionId) {
        return new AdminOperationsCostsService(tenantId, clientId, clientSecret, subscriptionId, "");
    }
}

