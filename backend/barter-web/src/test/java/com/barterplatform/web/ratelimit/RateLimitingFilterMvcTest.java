package com.barterplatform.web.ratelimit;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.web.security.jwt.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class RateLimitingFilterMvcTest {

    private static final UUID USER_ONE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_TWO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private RateLimitProperties properties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setLogin(new RateLimitProperties.Policy(2, Duration.ofMinutes(1)));
        properties.setTradeOfferCreate(new RateLimitProperties.Policy(1, Duration.ofMinutes(1)));
        properties.setFavoriteMutation(new RateLimitProperties.Policy(1, Duration.ofMinutes(1)));

        RateLimitingFilter filter = new RateLimitingFilter(
                properties,
                new RateLimitService(),
                new ObjectMapper().findAndRegisterModules());

        mockMvc = MockMvcBuilders.standaloneSetup(new TestRateLimitedController())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowRequestsUntilAnonymousIpLimitIsExceeded() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please retry later."))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void shouldUseConfiguredTrustedClientIpHeaderWhenPresent() throws Exception {
        properties.setClientIpHeader("X-Forwarded-For");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.5, 10.0.0.1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.5, 10.0.0.1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.5, 10.0.0.1"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldKeyAuthenticatedActionsByUserAndIp() throws Exception {
        authenticate(USER_ONE);
        mockMvc.perform(post("/api/v1/trade-offers").with(request -> {
                    request.setRemoteAddr("203.0.113.20");
                    return request;
                }))
                .andExpect(status().isOk());

        authenticate(USER_ONE);
        mockMvc.perform(post("/api/v1/trade-offers").with(request -> {
                    request.setRemoteAddr("203.0.113.20");
                    return request;
                }))
                .andExpect(status().isTooManyRequests());

        authenticate(USER_TWO);
        mockMvc.perform(post("/api/v1/trade-offers").with(request -> {
                    request.setRemoteAddr("203.0.113.20");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIgnoreUnscopedEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/items"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/catalog/items"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/catalog/items"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSupportOptionalFavoriteMutationLimit() throws Exception {
        authenticate(USER_ONE);
        mockMvc.perform(post("/api/v1/catalog/items/%s/favorite".formatted(UUID.randomUUID())))
                .andExpect(status().isOk());

        authenticate(USER_ONE);
        mockMvc.perform(post("/api/v1/catalog/items/%s/favorite".formatted(UUID.randomUUID())))
                .andExpect(status().isTooManyRequests());
    }

    private void authenticate(UUID userUuid) {
        AuthenticatedUser user = new AuthenticatedUser(userUuid, "user-" + userUuid, List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @RestController
    static class TestRateLimitedController {

        @PostMapping({
                "/api/v1/auth/login",
                "/api/v1/trade-offers",
                "/api/v1/catalog/items",
                "/api/v1/catalog/items/{itemUuid}/favorite"
        })
        ResponseEntity<String> postOk(@PathVariable(required = false) UUID itemUuid) {
            return ResponseEntity.ok(itemUuid == null ? "ok" : "ok-" + itemUuid);
        }

        @DeleteMapping("/api/v1/catalog/items/{itemUuid}/favorite")
        ResponseEntity<String> deleteOk(@PathVariable UUID itemUuid) {
            return ResponseEntity.ok("ok-" + itemUuid);
        }
    }
}

