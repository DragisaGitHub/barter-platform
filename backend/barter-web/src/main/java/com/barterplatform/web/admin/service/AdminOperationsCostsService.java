package com.barterplatform.web.admin.service;

import com.barterplatform.api.model.AdminOperationsCostsDailyEntry;
import com.barterplatform.api.model.AdminOperationsCostsResponse;
import com.barterplatform.api.model.AdminOperationsCostsServiceEntry;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Provides Azure Cost Management data for the Admin Operations Center.
 *
 * <p>When {@code AZURE_TENANT_ID}, {@code AZURE_CLIENT_ID}, {@code AZURE_CLIENT_SECRET},
 * and {@code AZURE_SUBSCRIPTION_ID} are configured, this service authenticates via the
 * Microsoft identity platform client credentials flow and queries the Azure Cost Management
 * Query API (2025-03-01) to return current month, previous month, and per-service cost data.
 *
 * <p>Credentials and access tokens are <strong>never</strong> logged or included in any response.
 * A safe placeholder is returned when configuration is missing or when the Azure API call fails.
 *
 * <p>Results are cached in-memory for {@value #COSTS_CACHE_TTL_MINUTES} minutes to avoid
 * calling Azure on every admin page load.
 */
@Service
public class AdminOperationsCostsService {

    private static final Logger log = LoggerFactory.getLogger(AdminOperationsCostsService.class);

    // ── Azure endpoints ──────────────────────────────────────────────────────

    private static final String TOKEN_URL_TEMPLATE =
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String COST_API_URL_TEMPLATE =
            "https://management.azure.com%s/providers/Microsoft.CostManagement/query?api-version=2025-03-01";
    private static final String MGMT_SCOPE = "https://management.azure.com/.default";

    // ── Cache TTLs ───────────────────────────────────────────────────────────

    static final int COSTS_CACHE_TTL_MINUTES = 15;
    private static final Duration COSTS_CACHE_TTL = Duration.ofMinutes(COSTS_CACHE_TTL_MINUTES);
    private static final Duration TOKEN_EXPIRY_BUFFER = Duration.ofMinutes(5);

    // ── Availability strings ─────────────────────────────────────────────────

    private static final String AVAILABILITY_CONFIGURED = "configured";
    private static final String AVAILABILITY_PLACEHOLDER = "placeholder";
    private static final String AVAILABILITY_UNAVAILABLE = "unavailable";

    // ── Config ───────────────────────────────────────────────────────────────

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String resolvedScope;

    // ── HTTP client ──────────────────────────────────────────────────────────

    private final RestClient restClient;

    // ── In-memory caches ─────────────────────────────────────────────────────

    private volatile TokenCache tokenCache;
    private volatile CostsCache costsCache;

    public AdminOperationsCostsService(
            @Value("${barter.azure.cost.tenant-id:}") String tenantId,
            @Value("${barter.azure.cost.client-id:}") String clientId,
            @Value("${barter.azure.cost.client-secret:}") String clientSecret,
            @Value("${barter.azure.cost.subscription-id:}") String subscriptionId,
            @Value("${barter.azure.cost.scope:}") String costScope) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.resolvedScope = resolveScope(costScope, subscriptionId);
        this.restClient = buildRestClient();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public AdminOperationsCostsResponse getCosts() {
        if (!isConfigured()) {
            log.debug("Azure Cost Management configuration is absent; returning placeholder.");
            return placeholder();
        }

        CostsCache cached = costsCache;
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached Azure Cost Management data (age < {} min).", COSTS_CACHE_TTL_MINUTES);
            return cached.response();
        }

        try {
            AdminOperationsCostsResponse response = fetchFromAzure();
            costsCache = new CostsCache(response, Instant.now());
            return response;
        } catch (RestClientException ex) {
            log.warn(
                    "Azure Cost Management HTTP call failed: exceptionClass={} message={}",
                    ex.getClass().getName(),
                    sanitizeMessage(ex.getMessage()));
            return unavailable("Azure Cost Management is currently unavailable.");
        } catch (Exception ex) {
            log.warn(
                    "Azure Cost Management query failed: exceptionClass={} rootCause={} message={}",
                    ex.getClass().getName(),
                    rootCauseClass(ex),
                    sanitizeMessage(rootCauseMessage(ex)));
            return unavailable("Azure Cost Management is currently unavailable.");
        }
    }

    // ── Private: fetch logic ─────────────────────────────────────────────────

    private AdminOperationsCostsResponse fetchFromAzure() {
        String token = acquireAccessToken();

        JsonNode currentMonthResult = queryApi(token, buildCurrentMonthRequest());
        JsonNode previousMonthResult = queryApi(token, buildPreviousMonthRequest());

        return parseResponse(currentMonthResult, previousMonthResult);
    }

    private String acquireAccessToken() {
        TokenCache cached = tokenCache;
        if (cached != null && cached.isValid()) {
            return cached.accessToken();
        }

        String tokenUrl = TOKEN_URL_TEMPLATE.formatted(tenantId);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", MGMT_SCOPE);

        JsonNode tokenResponse = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (tokenResponse == null || tokenResponse.path("access_token").isMissingNode()) {
            throw new IllegalStateException("Token response did not contain access_token.");
        }

        String accessToken = tokenResponse.get("access_token").textValue();
        long expiresIn = tokenResponse.path("expires_in").asLong(3600L);
        Instant expiresAt = Instant.now().plusSeconds(expiresIn).minus(TOKEN_EXPIRY_BUFFER);

        tokenCache = new TokenCache(accessToken, expiresAt);
        return accessToken;
    }

    private JsonNode queryApi(String token, Map<String, Object> requestBody) {
        String url = COST_API_URL_TEMPLATE.formatted(resolvedScope);
        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);
    }

    // ── Private: request builders ────────────────────────────────────────────

    private static Map<String, Object> buildCurrentMonthRequest() {
        return Map.of(
                "type", "ActualCost",
                "timeframe", "MonthToDate",
                "dataset", Map.of(
                        "granularity", "Daily",
                        "aggregation", Map.of(
                                "totalCost", Map.of("name", "Cost", "function", "Sum")),
                        "grouping", List.of(
                                Map.of("type", "Dimension", "name", "ServiceName"))
                )
        );
    }

    private static Map<String, Object> buildPreviousMonthRequest() {
        return Map.of(
                "type", "ActualCost",
                "timeframe", "TheLastMonth",
                "dataset", Map.of(
                        "granularity", "None",
                        "aggregation", Map.of(
                                "totalCost", Map.of("name", "Cost", "function", "Sum")),
                        "grouping", List.of(
                                Map.of("type", "Dimension", "name", "ServiceName"))
                )
        );
    }

    // ── Private: response parsing ────────────────────────────────────────────

    private AdminOperationsCostsResponse parseResponse(JsonNode current, JsonNode previous) {
        JsonNode currentProps = current == null ? null : current.path("properties");
        JsonNode previousProps = previous == null ? null : previous.path("properties");

        if (currentProps == null || currentProps.isMissingNode()) {
            log.warn("Azure Cost Management response missing 'properties' node.");
            return unavailable("Azure Cost Management returned an unexpected response format.");
        }

        JsonNode columns = currentProps.path("columns");
        JsonNode rows = currentProps.path("rows");

        int costIdx = columnIndex(columns, "Cost");
        int serviceIdx = columnIndex(columns, "ServiceName");
        int dateIdx = columnIndex(columns, "UsageDate");
        int currencyIdx = columnIndex(columns, "Currency");

        if (costIdx < 0) {
            log.warn("Cost column not found in Azure Cost Management response.");
            return unavailable("Azure Cost Management returned an unexpected response format.");
        }

        String currency = extractCurrency(rows, currencyIdx);
        double currentMonthCost = 0.0;

        // Per-service totals (current month)
        Map<String, Double> serviceTotals = new LinkedHashMap<>();
        // Per-date totals (current month)
        Map<String, Double> dateTotals = new LinkedHashMap<>();

        for (JsonNode row : rows) {
            if (row.size() <= costIdx) continue;
            double cost = row.get(costIdx).asDouble();
            currentMonthCost += cost;

            if (serviceIdx >= 0 && row.size() > serviceIdx) {
                String rawSvc = row.get(serviceIdx).stringValue();
                String svc = (rawSvc != null && !rawSvc.isBlank()) ? rawSvc : "Unknown";
                serviceTotals.merge(svc, cost, Double::sum);
            }

            if (dateIdx >= 0 && row.size() > dateIdx) {
                String dateStr = formatUsageDate(row.get(dateIdx).asLong());
                if (dateStr != null) {
                    dateTotals.merge(dateStr, cost, Double::sum);
                }
            }
        }

        double previousMonthCost = extractPreviousMonthCost(previousProps);
        Double projectedMonthCost = projectCurrentMonth(currentMonthCost);

        List<AdminOperationsCostsDailyEntry> dailyTrend = buildDailyTrend(dateTotals);
        List<AdminOperationsCostsServiceEntry> serviceBreakdown = buildServiceBreakdown(serviceTotals, currency);

        return new AdminOperationsCostsResponse()
                .availability(AVAILABILITY_CONFIGURED)
                .currency(currency)
                .currentMonthCost(currentMonthCost)
                .previousMonthCost(previousMonthCost > 0 ? previousMonthCost : null)
                .projectedMonthCost(projectedMonthCost)
                .dailyTrend(dailyTrend)
                .serviceBreakdown(serviceBreakdown)
                .scope(resolvedScope)
                .lastUpdated(OffsetDateTime.now(ZoneOffset.UTC))
                .note(null);
    }

    private static double extractPreviousMonthCost(JsonNode previousProps) {
        if (previousProps == null || previousProps.isMissingNode()) return 0.0;
        JsonNode prevColumns = previousProps.path("columns");
        JsonNode prevRows = previousProps.path("rows");
        int prevCostIdx = columnIndex(prevColumns, "Cost");
        if (prevCostIdx < 0) return 0.0;
        double total = 0.0;
        for (JsonNode row : prevRows) {
            if (row.size() > prevCostIdx) {
                total += row.get(prevCostIdx).asDouble();
            }
        }
        return total;
    }

    /**
     * Simple linear projection: (cost so far / days elapsed) * days in month.
     * Returns null if today is the first day or data is insufficient.
     */
    private static Double projectCurrentMonth(double currentMonthCost) {
        if (currentMonthCost <= 0) return null;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int dayOfMonth = today.getDayOfMonth();
        if (dayOfMonth <= 1) return null;
        int daysInMonth = YearMonth.of(today.getYear(), today.getMonth()).lengthOfMonth();
        return (currentMonthCost / dayOfMonth) * daysInMonth;
    }

    private static List<AdminOperationsCostsDailyEntry> buildDailyTrend(Map<String, Double> dateTotals) {
        List<AdminOperationsCostsDailyEntry> trend = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dateTotals.entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey(), DateTimeFormatter.ISO_LOCAL_DATE);
                trend.add(new AdminOperationsCostsDailyEntry()
                        .date(date)
                        .cost(entry.getValue()));
            } catch (Exception ignored) {
                // skip unparseable date
            }
        }
        trend.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return trend.isEmpty() ? null : trend;
    }

    private static List<AdminOperationsCostsServiceEntry> buildServiceBreakdown(
            Map<String, Double> serviceTotals, String currency) {
        if (serviceTotals.isEmpty()) return null;
        List<AdminOperationsCostsServiceEntry> breakdown = new ArrayList<>();
        for (Map.Entry<String, Double> entry : serviceTotals.entrySet()) {
            breakdown.add(new AdminOperationsCostsServiceEntry()
                    .serviceName(entry.getKey())
                    .cost(entry.getValue())
                    .currency(currency != null ? currency : "USD"));
        }
        breakdown.sort((a, b) -> Double.compare(b.getCost(), a.getCost()));
        return breakdown;
    }

    // ── Private: helpers ─────────────────────────────────────────────────────

    private static int columnIndex(JsonNode columns, String name) {
        if (columns == null || columns.isMissingNode()) return -1;
        for (int i = 0; i < columns.size(); i++) {
            JsonNode col = columns.get(i);
            if (col != null && name.equals(col.path("name").textValue())) {
                return i;
            }
        }
        return -1;
    }

    private static String extractCurrency(JsonNode rows, int currencyIdx) {
        if (currencyIdx < 0) return "USD";
        for (JsonNode row : rows) {
            if (row.size() > currencyIdx) {
                String currency = row.get(currencyIdx).textValue();
                if (currency != null && !currency.isBlank()) {
                    return currency;
                }
            }
        }
        return "USD";
    }

    /**
     * Converts Azure's integer UsageDate (yyyyMMdd) to an ISO date string.
     * Returns null if the value cannot be parsed.
     */
    static String formatUsageDate(long usageDateInt) {
        if (usageDateInt <= 0) return null;
        String s = String.valueOf(usageDateInt);
        if (s.length() != 8) return null;
        try {
            int year = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(4, 6));
            int day = Integer.parseInt(s.substring(6, 8));
            return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isConfigured() {
        return !isBlank(tenantId)
                && !isBlank(clientId)
                && !isBlank(clientSecret)
                && !isBlank(resolvedScope);
    }

    private static String resolveScope(String scope, String subscriptionId) {
        if (!isBlank(scope)) return scope.trim();
        if (!isBlank(subscriptionId)) return "/subscriptions/" + subscriptionId.trim();
        return "";
    }

    private AdminOperationsCostsResponse placeholder() {
        return new AdminOperationsCostsResponse()
                .availability(AVAILABILITY_PLACEHOLDER)
                .note("Azure Cost Management configuration is not present in this environment. "
                        + "Set AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, and "
                        + "AZURE_SUBSCRIPTION_ID to enable real cost visibility.");
    }

    private AdminOperationsCostsResponse unavailable(String note) {
        return new AdminOperationsCostsResponse()
                .availability(AVAILABILITY_UNAVAILABLE)
                .note(note);
    }

    private static RestClient buildRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) return "n/a";
        // Redact subscription IDs, client secrets, and tokens that may appear in error messages.
        return message
                .replaceAll("(?i)(client_secret=)[^&\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)(access_token=)[^&\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)(AccountKey=)[^;\\s]+", "$1[REDACTED]")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer [REDACTED]");
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

    // ── Cache records ────────────────────────────────────────────────────────

    private record TokenCache(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private record CostsCache(AdminOperationsCostsResponse response, Instant cachedAt) {
        boolean isExpired() {
            return Duration.between(cachedAt, Instant.now()).compareTo(COSTS_CACHE_TTL) >= 0;
        }
    }
}

