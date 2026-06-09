package com.barterplatform.web.catalog.controller;

import com.barterplatform.application.catalog.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Enumeration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@RestController
public class StoredFileController {

    private static final String FILES_PATTERN = "/files/**";
    private static final String IMMUTABLE_IMAGE_CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final FileStorageService storageService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public StoredFileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping(FILES_PATTERN)
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) throws IOException {
        String storageKey = extractStorageKey(request);

        try {
            FileStorageService.StoredFileMetadata metadata = storageService.getMetadata(storageKey);
            if (isNotModified(request, metadata)) {
                return responseBuilder(HttpStatus.NOT_MODIFIED, metadata).build();
            }

            FileStorageService.StoredFile file = storageService.load(storageKey);
            return responseBuilder(HttpStatus.OK, file.metadata())
                    .contentType(resolveMediaType(file.contentType()))
                    .contentLength(file.contentLength())
                    .body(file.content());
        } catch (NoSuchFileException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private ResponseEntity.BodyBuilder responseBuilder(HttpStatus status, FileStorageService.StoredFileMetadata metadata) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, IMMUTABLE_IMAGE_CACHE_CONTROL)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff");

        String etag = normalizeEtag(metadata.etag());
        if (etag != null) {
            builder.header(HttpHeaders.ETAG, etag);
        }

        Instant lastModified = metadata.lastModified();
        if (lastModified != null) {
            builder.lastModified(toHttpDateMillis(lastModified));
        }

        return builder;
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    private boolean isNotModified(HttpServletRequest request, FileStorageService.StoredFileMetadata metadata) {
        String etag = normalizeEtag(metadata.etag());
        if (etag != null && matchesIfNoneMatch(request, etag)) {
            return true;
        }

        if (hasIfNoneMatch(request)) {
            return false;
        }

        long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince < 0 || metadata.lastModified() == null) {
            return false;
        }

        return toHttpDateMillis(metadata.lastModified()) <= ifModifiedSince;
    }

    private boolean hasIfNoneMatch(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.IF_NONE_MATCH) != null;
    }

    private boolean matchesIfNoneMatch(HttpServletRequest request, String etag) {
        Enumeration<String> headerValues = request.getHeaders(HttpHeaders.IF_NONE_MATCH);
        while (headerValues.hasMoreElements()) {
            String headerValue = headerValues.nextElement();
            if (headerValue == null || headerValue.isBlank()) {
                continue;
            }

            boolean matches = Arrays.stream(headerValue.split(","))
                    .map(String::trim)
                    .anyMatch(candidate -> "*".equals(candidate) || weakEtagEquals(candidate, etag));
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private boolean weakEtagEquals(String candidate, String current) {
        return stripEtagDecorators(candidate).equals(stripEtagDecorators(current));
    }

    private String stripEtagDecorators(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("W/")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeEtag(String etag) {
        if (etag == null || etag.isBlank()) {
            return null;
        }

        String normalized = etag.trim();
        boolean weak = false;
        if (normalized.startsWith("W/")) {
            weak = true;
            normalized = normalized.substring(2).trim();
        }
        if (!(normalized.startsWith("\"") && normalized.endsWith("\""))) {
            normalized = '"' + normalized + '"';
        }
        return weak ? "W/" + normalized : normalized;
    }

    private long toHttpDateMillis(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
    }

    private String extractStorageKey(HttpServletRequest request) {
        String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pathMatcher.extractPathWithinPattern(bestMatchingPattern, pathWithinMapping);
    }
}


