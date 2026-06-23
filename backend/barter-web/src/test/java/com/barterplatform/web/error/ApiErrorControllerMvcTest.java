package com.barterplatform.web.error;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.web.observability.CorrelationIdFilter;
import jakarta.servlet.RequestDispatcher;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiErrorControllerMvcTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ApiErrorController()).build();

    @ParameterizedTest
    @MethodSource("errorMappings")
    void shouldReturnConsistentJsonErrorResponse(
            int statusCode,
            String error,
            String code,
            String message,
            String path) throws Exception {

        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, statusCode)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, path)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "unsafe internal details should not be exposed"))
                .andExpect(status().is(statusCode))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(statusCode))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void shouldFallbackToInternalErrorAndAvoidLeakingUnsafeDetails() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, "not-a-number")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "SQL grammar exception in users table"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred."))
                .andExpect(jsonPath("$.path").value("/error"))
                .andExpect(jsonPath("$.message", not(org.hamcrest.Matchers.containsString("SQL"))))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void shouldIncludeRequestIdFromCorrelationIdAttribute() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/missing")
                        .requestAttr(CorrelationIdFilter.CORRELATION_ID_REQUEST_ATTRIBUTE, "req-id-12345678"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.requestId").value("req-id-12345678"));
    }

    @Test
    void shouldFallbackToMdcCorrelationIdWhenAttributeIsAbsent() throws Exception {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "mdc-id-12345678");
        try {
            mockMvc.perform(get("/error")
                            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                            .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/missing"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.requestId").value("mdc-id-12345678"));
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        }
    }

    @Test
    void shouldOmitRequestIdWhenNeitherAttributeNorMdcIsPresent() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    private static Stream<Arguments> errorMappings() {
        return Stream.of(
                Arguments.of(400, "Bad Request", "BAD_REQUEST", "The request could not be understood. Please verify the submitted data.", "/api/v1/catalog/items"),
                Arguments.of(401, "Unauthorized", "UNAUTHORIZED", "Authentication is required to access this resource.", "/api/v1/auth/me"),
                Arguments.of(403, "Forbidden", "FORBIDDEN", "Access is denied.", "/api/v1/admin/users"),
                Arguments.of(404, "Not Found", "NOT_FOUND", "The requested resource was not found.", "/api/v1/missing"),
                Arguments.of(500, "Internal Server Error", "INTERNAL_ERROR", "An unexpected internal error occurred.", "/api/v1/error-case")
        );
    }
}

