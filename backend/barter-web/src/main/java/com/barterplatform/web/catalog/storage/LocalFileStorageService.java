package com.barterplatform.web.catalog.storage;

import com.barterplatform.application.catalog.storage.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path basePath;

    public LocalFileStorageService(
            @Value("${barter.storage.local.base-path:./uploads}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream inputStream, String contentType) throws IOException {
        Path destination = resolveAndEnsureParent(storageKey);
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored file at {}", destination);
    }

    @Override
    public void delete(String storageKey) {
        Path target = basePath.resolve(storageKey).normalize();
        try {
            Files.deleteIfExists(target);
            log.debug("Deleted file at {}", target);
        } catch (IOException e) {
            log.warn("Could not delete file at {}: {}", target, e.getMessage());
        }
    }

    @Override
    public String resolveUrl(String storageKey) {
        // Files are served publicly under /files/<storageKey>
        // Build URL relative to current request context when available,
        // otherwise fall back to a relative path that works with the configured context-path.
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/files/")
                    .path(storageKey)
                    .toUriString();
        } catch (IllegalStateException e) {
            // No current request (e.g. during tests) – return a relative sentinel
            return "/files/" + storageKey;
        }
    }

    // ── private helpers ──────────────────────────────────────────

    private Path resolveAndEnsureParent(String storageKey) throws IOException {
        Path destination = basePath.resolve(storageKey).normalize();
        if (!destination.startsWith(basePath)) {
            throw new IOException("Storage key attempts directory traversal: " + storageKey);
        }
        Files.createDirectories(destination.getParent());
        return destination;
    }
}

