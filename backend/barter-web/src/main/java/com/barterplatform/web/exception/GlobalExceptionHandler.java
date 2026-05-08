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

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new FieldErrorResponse(fieldError.getField(), resolveMessage(fieldError.getDefaultMessage())));
        }

        ex.getBindingResult().getGlobalErrors().forEach(globalError ->
                fieldErrors.add(new FieldErrorResponse(globalError.getObjectName(), resolveMessage(globalError.getDefaultMessage()))));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Request validation failed.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "request",
                        resolveMessage(violation.getMessage())))
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Request validation failed.",
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
                resolveMessage(ex.getMessage(), "Access is denied."),
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
                resolveMessage(ex.getMessage(), "Authentication is required to access this resource."),
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "An unexpected internal error occurred.",
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
                .message(resolveMessage(message))
                .path(request.getRequestURI())
                .fieldErrors(new ArrayList<>(fieldErrors));

        return ResponseEntity.status(status).body(errorResponse);
    }

    private String resolveMessage(String message) {
        return resolveMessage(message, "Request could not be processed.");
    }

    private String resolveMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
