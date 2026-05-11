package com.barterplatform.application.catalog.storage;

import java.io.IOException;
import java.io.InputStream;

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
    void store(String storageKey, InputStream inputStream, String contentType) throws IOException;

    /**
     * Delete the file identified by storageKey. Silently succeeds if the file
     * does not exist.
     *
     * @param storageKey relative path/key for the file
     */
    void delete(String storageKey);

    /**
     * Resolve the public URL for a given storage key.
     *
     * @param storageKey relative path/key for the file
     * @return publicly accessible URL
     */
    String resolveUrl(String storageKey);
}

