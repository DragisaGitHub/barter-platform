package com.barterplatform.application.catalog.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

/**
 * Abstraction for file storage. Implementations can be local filesystem,
 * S3, Azure Blob, etc.
 */
public interface FileStorageService {

    /**
     * Store the given input stream under the given key.
     *
     * @param storageKey  relative path/key for the file
     * @param inputStream content to store
     * @param contentType MIME type of the content
     * @throws IOException if the write fails
     */
    void store(String storageKey, InputStream inputStream, long contentLength, String contentType) throws IOException;

    /**
     * Delete the file identified by storageKey. Silently succeeds if the file
     * does not exist.
     *
     * @param storageKey relative path/key for the file
     */
    void delete(String storageKey);

    /**
     * Resolve the application URL for a given storage key.
     *
     * <p>Implementations should return the backend-served file endpoint rather than exposing
     * provider-native blob URLs directly. This keeps object-storage containers private while the
     * application remains the stable public access path.</p>
     *
     * @param storageKey relative path/key for the file
     * @return application URL that can be used to fetch the stored file through the backend
     */
    String resolveUrl(String storageKey);

    /**
     * Load file contents for backend streaming.
     *
     * @param storageKey relative path/key for the file
     * @return stored file payload and metadata
     * @throws NoSuchFileException if the file does not exist
     * @throws IOException if the read fails
     */
    StoredFile load(String storageKey) throws IOException;

    record StoredFile(byte[] content, String contentType) {
        public long contentLength() {
            return content.length;
        }
    }
}

