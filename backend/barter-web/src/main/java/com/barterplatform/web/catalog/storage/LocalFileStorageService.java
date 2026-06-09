package com.barterplatform.web.catalog.storage;

import com.barterplatform.application.catalog.storage.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@Profile({"local", "default"})
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path basePath;

    public LocalFileStorageService(
            @Value("${barter.storage.local.base-path:${storage.local.upload-dir:./uploads}}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream inputStream, long contentLength, String contentType) throws IOException {
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

    @Override
    public StoredFileMetadata getMetadata(String storageKey) throws IOException {
        Path target = resolveExisting(storageKey);
        return readMetadata(target, storageKey);
    }

    @Override
    public StoredFile load(String storageKey) throws IOException {
        Path target = resolveExisting(storageKey);
        StoredFileMetadata metadata = readMetadata(target, storageKey);
        return new StoredFile(Files.readAllBytes(target), metadata.contentType(), metadata.etag(), metadata.lastModified());
    }

    // ── private helpers ──────────────────────────────────────────

    private Path resolve(String storageKey) throws IOException {
        Path destination = basePath.resolve(storageKey).normalize();
        if (!destination.startsWith(basePath)) {
            throw new IOException("Storage key attempts directory traversal: " + storageKey);
        }
        return destination;
    }

    private Path resolveExisting(String storageKey) throws IOException {
        Path target = resolve(storageKey);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new NoSuchFileException(storageKey);
        }
        return target;
    }

    private StoredFileMetadata readMetadata(Path target, String storageKey) throws IOException {
        String contentType = Files.probeContentType(target);
        if (contentType == null || contentType.isBlank()) {
            contentType = fallbackContentType(storageKey);
        }

        long contentLength = Files.size(target);
        FileTime lastModifiedTime = Files.getLastModifiedTime(target);
        Instant lastModified = lastModifiedTime.toInstant();
        String etag = "W/\"%s-%s\"".formatted(Long.toHexString(contentLength), Long.toHexString(lastModifiedTime.toMillis()));
        return new StoredFileMetadata(contentType, contentLength, etag, lastModified);
    }

    private String fallbackContentType(String storageKey) {
        String normalizedKey = storageKey.toLowerCase();
        if (normalizedKey.endsWith(".jpg") || normalizedKey.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalizedKey.endsWith(".png")) {
            return "image/png";
        }
        if (normalizedKey.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private Path resolveAndEnsureParent(String storageKey) throws IOException {
        Path destination = resolve(storageKey);
        Files.createDirectories(destination.getParent());
        return destination;
    }
}

