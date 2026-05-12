package com.barterplatform.web.exception;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Iterator;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerMvcTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldMapApiExceptionToStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/api"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Duplicate resource."))
                .andExpect(jsonPath("$.path").value("/test-exceptions/api"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void shouldMapMethodArgumentNotValidExceptionToValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.path").value("/test-exceptions/validation"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", not(empty())))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    void shouldMapGenericExceptionToInternalErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred."))
                .andExpect(jsonPath("$.path").value("/test-exceptions/generic"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void shouldMapConstraintViolationExceptionToValidationErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.path").value("/test-exceptions/constraint"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", not(empty())))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("query"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
    }

    @Test
    void shouldMapAccessDeniedExceptionToForbiddenErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Access to this resource is denied."))
                .andExpect(jsonPath("$.path").value("/test-exceptions/access-denied"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void shouldMapAuthenticationExceptionToUnauthorizedErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/authentication"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication failed."))
                .andExpect(jsonPath("$.path").value("/test-exceptions/authentication"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @RestController
    @RequestMapping("/test-exceptions")
    static class TestExceptionController {

        @RequestMapping("/api")
        String apiException() {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Duplicate resource.");
        }

        @PostMapping("/validation")
        String validation(@Valid @RequestBody ValidationRequest request) {
            return request.name();
        }

        @RequestMapping("/generic")
        String genericException() {
            throw new IllegalStateException("Unexpected failure.");
        }

        @RequestMapping("/constraint")
        String constraintViolation() {
            throw new ConstraintViolationException(Set.of(mockConstraintViolation()));
        }

        @RequestMapping("/access-denied")
        String accessDenied() {
            throw new AccessDeniedException("Access to this resource is denied.");
        }

        @RequestMapping("/authentication")
        String authentication() {
            throw new BadCredentialsException("Authentication failed.");
        }
    }

    record ValidationRequest(@NotBlank(message = "must not be blank") String name) {
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<Object> mockConstraintViolation() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = new SimplePath("query");

        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        return violation;
    }

    private record SimplePath(String value) implements Path {

        @Override
        public @NonNull Iterator<Node> iterator() {
            return java.util.List.<Node>of().iterator();
        }

        @Override
        public @NonNull String toString() {
            return value;
        }
    }
}
