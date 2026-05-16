package com.barterplatform.web.catalog.storage;

import com.barterplatform.application.catalog.storage.FileStorageService;
import java.io.ByteArrayInputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesLoadsAndDeletesFile() throws Exception {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
        byte[] content = "test-image".getBytes();
        String storageKey = "items/demo/test.jpg";

        service.store(storageKey, new ByteArrayInputStream(content), content.length, "image/jpeg");

        FileStorageService.StoredFile stored = service.load(storageKey);
        assertArrayEquals(content, stored.content());
        assertEquals("image/jpeg", stored.contentType());

        service.delete(storageKey);
        assertThrows(NoSuchFileException.class, () -> service.load(storageKey));
    }
}

