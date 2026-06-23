package com.barterplatform.web.error;

import com.barterplatform.api.model.ErrorResponse;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.observability.CorrelationIdFilter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.slf4j.MDC;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ApiErrorController implements ErrorController {

    private static final String DEFAULT_BAD_REQUEST_MESSAGE = "The request could not be understood. Please verify the submitted data.";
    private static final String DEFAULT_UNAUTHORIZED_MESSAGE = "Authentication is required to access this resource.";
    private static final String DEFAULT_FORBIDDEN_MESSAGE = "Access is denied.";
    private static final String DEFAULT_NOT_FOUND_MESSAGE = "The requested resource was not found.";
    private static final String DEFAULT_INTERNAL_MESSAGE = "An unexpected internal error occurred.";

    @RequestMapping
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(resolveCode(status).name())
                .message(resolveMessage(status))
                .path(resolvePath(request))
                .fieldErrors(new ArrayList<>());

        String requestId = resolveRequestId(request);
        if (requestId != null) {
            errorResponse.requestId(requestId);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_REQUEST_ATTRIBUTE);
        if (attr instanceof String value && !value.isBlank()) {
            return value;
        }
        String mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (mdcValue != null && !mdcValue.isBlank()) {
            return mdcValue;
        }
        return null;
    }

    private HttpStatus resolveStatus(Object statusCodeAttribute) {
        Integer statusCode = null;

        if (statusCodeAttribute instanceof Integer value) {
            statusCode = value;
        } else if (statusCodeAttribute instanceof String value && !value.isBlank()) {
            try {
                statusCode = Integer.parseInt(value);
            } catch (NumberFormatException ignored) {

            }
        }

        HttpStatus status = statusCode != null ? HttpStatus.resolve(statusCode) : null;
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ErrorCode resolveCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    private String resolveMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> DEFAULT_BAD_REQUEST_MESSAGE;
            case UNAUTHORIZED -> DEFAULT_UNAUTHORIZED_MESSAGE;
            case FORBIDDEN -> DEFAULT_FORBIDDEN_MESSAGE;
            case NOT_FOUND -> DEFAULT_NOT_FOUND_MESSAGE;
            default -> DEFAULT_INTERNAL_MESSAGE;
        };
    }

    private String resolvePath(HttpServletRequest request) {
        Object errorRequestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorRequestUri instanceof String path && !path.isBlank()) {
            return path;
        }

        String requestUri = request.getRequestURI();
        return requestUri != null && !requestUri.isBlank() ? requestUri : "/error";
    }
}

