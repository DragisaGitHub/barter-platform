package com.barterplatform.web.catalog.controller;

import com.barterplatform.application.catalog.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import org.springframework.http.CacheControl;
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

    private final FileStorageService storageService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public StoredFileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping(FILES_PATTERN)
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) throws IOException {
        String storageKey = extractStorageKey(request);

        try {
            FileStorageService.StoredFile file = storageService.load(storageKey);
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (file.contentType() != null && !file.contentType().isBlank()) {
                mediaType = MediaType.parseMediaType(file.contentType());
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(file.contentLength())
                    .cacheControl(CacheControl.noCache())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(file.content());
        } catch (NoSuchFileException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private String extractStorageKey(HttpServletRequest request) {
        String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pathMatcher.extractPathWithinPattern(bestMatchingPattern, pathWithinMapping);
    }
}


