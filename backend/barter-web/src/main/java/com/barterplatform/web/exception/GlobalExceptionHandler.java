package com.barterplatform.web.exception;

import com.barterplatform.api.model.ErrorResponse;
import com.barterplatform.api.model.FieldErrorResponse;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_VALIDATION_MESSAGE = "Please check the entered fields.";
    private static final String DEFAULT_PROCESSING_MESSAGE = "Request could not be processed.";
    private static final String DEFAULT_INTERNAL_MESSAGE = "An unexpected internal error occurred.";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {
        return buildResponse(
                ex.getStatus(),
                ex.getCode(),
                sanitizeMessage(ex.getMessage(), DEFAULT_PROCESSING_MESSAGE),
                request,
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldErrorResponse> fieldErrors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new FieldErrorResponse(
                    fieldError.getField(),
                    sanitizeMessage(fieldError.getDefaultMessage(), DEFAULT_VALIDATION_MESSAGE)));
        }

        ex.getBindingResult().getGlobalErrors().forEach(globalError ->
                fieldErrors.add(new FieldErrorResponse(
                        globalError.getObjectName(),
                        sanitizeMessage(globalError.getDefaultMessage(), DEFAULT_VALIDATION_MESSAGE)))
        );

        String topLevelMessage = fieldErrors.isEmpty()
                ? DEFAULT_VALIDATION_MESSAGE
                : fieldErrors.getFirst().getMessage();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                topLevelMessage,
                request,
                fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<FieldErrorResponse> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath() != null
                                ? violation.getPropertyPath().toString()
                                : "request",
                        sanitizeMessage(violation.getMessage(), DEFAULT_VALIDATION_MESSAGE)))
                .toList();

        String topLevelMessage = fieldErrors.isEmpty()
                ? DEFAULT_VALIDATION_MESSAGE
                : fieldErrors.getFirst().getMessage();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                topLevelMessage,
                request,
                fieldErrors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                sanitizeMessage(ex.getMessage(), "Access is denied."),
                request,
                List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                sanitizeMessage(ex.getMessage(), "Authentication is required to access this resource."),
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                DEFAULT_INTERNAL_MESSAGE,
                request,
                List.of());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors) {

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code.name())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(new ArrayList<>(fieldErrors));

        return ResponseEntity.status(status).body(errorResponse);
    }

    private String sanitizeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }

        String normalized = message.toLowerCase(Locale.ROOT);

        List<String> unsafePatterns = List.of(
                "hibernate",
                "sql",
                "select ",
                "insert ",
                "update ",
                "delete ",
                "constraint",
                "table ",
                "column ",
                "relation ",
                "org.hibernate",
                "java.",
                "exception"
        );

        boolean unsafe = unsafePatterns.stream()
                .anyMatch(normalized::contains);

        return unsafe ? fallback : message;
    }
}