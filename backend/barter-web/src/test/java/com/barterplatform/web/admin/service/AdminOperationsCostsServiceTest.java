package com.barterplatform.web.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.barterplatform.api.model.AdminOperationsCostsResponse;
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
 * </ul>
 *
 * <p>Real Azure Cost Management API calls are not tested here.
 * Integration with Azure is verified in staging/production environments.
 */
class AdminOperationsCostsServiceTest {

    // ── Placeholder / missing config ─────────────────────────────────────────

    @Test
    void returnPlaceholderWhenAllConfigIsBlank() {
        AdminOperationsCostsService service = serviceWith("", "", "", "", "");

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
        AdminOperationsCostsService service = serviceWith("", "client-id", "secret", "sub-id", "");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    @Test
    void returnPlaceholderWhenClientIdMissing() {
        AdminOperationsCostsService service = serviceWith("tenant", "", "secret", "sub-id", "");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    @Test
    void returnPlaceholderWhenClientSecretMissing() {
        AdminOperationsCostsService service = serviceWith("tenant", "client-id", "", "sub-id", "");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    @Test
    void returnPlaceholderWhenSubscriptionIdAndScopeBothMissing() {
        AdminOperationsCostsService service = serviceWith("tenant", "client-id", "secret", "", "");

        assertThat(service.getCosts().getAvailability()).isEqualTo("placeholder");
    }

    // ── No secrets in placeholder response ──────────────────────────────────

    @Test
    void placeholderResponseDoesNotContainSecrets() {
        // Scope/subscriptionId is blank → service returns placeholder immediately
        // without making any network calls. The note must not expose credentials.
        AdminOperationsCostsResponse response =
                serviceWith("my-tenant", "my-client", "super-secret", "", "").getCosts();

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static AdminOperationsCostsService serviceWith(
            String tenantId, String clientId, String clientSecret,
            String subscriptionId, String scope) {
        return new AdminOperationsCostsService(tenantId, clientId, clientSecret, subscriptionId, scope);
    }
}

