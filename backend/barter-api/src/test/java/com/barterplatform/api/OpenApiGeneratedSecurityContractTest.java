package com.barterplatform.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.barterplatform.api.controller.AuthApi;
import com.barterplatform.api.model.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OpenApiGeneratedSecurityContractTest {

    @Test
    void protectedOperationsShouldReferenceBearerJwtSecurityScheme() throws NoSuchMethodException {
        Method method = AuthApi.class.getMethod("getCurrentUser");
        Operation operation = method.getAnnotation(Operation.class);

        assertNotNull(operation);
        assertTrue(Arrays.stream(operation.security())
                .map(SecurityRequirement::name)
                .anyMatch("bearerJwt"::equals));
    }

    @Test
    void publicOperationsShouldNotDeclareBearerJwtSecurityScheme() throws NoSuchMethodException {
        Method method = AuthApi.class.getMethod("login", LoginRequest.class);
        Operation operation = method.getAnnotation(Operation.class);

        assertNotNull(operation);
        assertFalse(Arrays.stream(operation.security())
                .map(SecurityRequirement::name)
                .anyMatch("bearerJwt"::equals));
    }
}

