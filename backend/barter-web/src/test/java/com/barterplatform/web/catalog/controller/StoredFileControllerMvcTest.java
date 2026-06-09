package com.barterplatform.web.catalog.controller;

import com.barterplatform.application.catalog.storage.FileStorageService;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoredFileControllerMvcTest {

    private MockMvc mockMvc;
    private FileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = mock(FileStorageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StoredFileController(storageService)).build();
    }

    @Test
    void shouldServeImageWithImmutableCacheHeaders() throws Exception {
        String storageKey = "items/demo/test.jpg";
        byte[] contentBytes = "blob-data".getBytes(StandardCharsets.UTF_8);
        Instant lastModified = Instant.parse("2026-06-09T10:15:30Z");

        when(storageService.getMetadata(storageKey)).thenReturn(
                new FileStorageService.StoredFileMetadata("image/jpeg", contentBytes.length, "\"etag-1\"", lastModified));
        when(storageService.load(storageKey)).thenReturn(
                new FileStorageService.StoredFile(contentBytes, "image/jpeg", "\"etag-1\"", lastModified));

        mockMvc.perform(get("/files/" + storageKey))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"etag-1\""))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists(HttpHeaders.LAST_MODIFIED))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(contentBytes));

        verify(storageService).getMetadata(storageKey);
        verify(storageService).load(storageKey);
    }

    @Test
    void shouldReturn304WhenIfNoneMatchMatches() throws Exception {
        String storageKey = "items/demo/test.jpg";
        Instant lastModified = Instant.parse("2026-06-09T10:15:30Z");

        when(storageService.getMetadata(storageKey)).thenReturn(
                new FileStorageService.StoredFileMetadata("image/jpeg", 9L, "\"etag-1\"", lastModified));

        mockMvc.perform(get("/files/" + storageKey)
                        .header(HttpHeaders.IF_NONE_MATCH, "W/\"etag-1\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"etag-1\""));

        verify(storageService).getMetadata(storageKey);
        verify(storageService, never()).load(storageKey);
    }

    @Test
    void shouldReturn304WhenIfModifiedSinceMatches() throws Exception {
        String storageKey = "items/demo/test.jpg";
        Instant lastModified = Instant.parse("2026-06-09T10:15:30Z");
        String ifModifiedSince = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.ofInstant(lastModified.plusSeconds(30), ZoneId.of("GMT")));

        when(storageService.getMetadata(storageKey)).thenReturn(
                new FileStorageService.StoredFileMetadata("image/jpeg", 9L, "\"etag-1\"", lastModified));

        mockMvc.perform(get("/files/" + storageKey)
                        .header(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince))
                .andExpect(status().isNotModified());

        verify(storageService).getMetadata(storageKey);
        verify(storageService, never()).load(storageKey);
    }

    @Test
    void shouldReturn404WhenFileDoesNotExist() throws Exception {
        String storageKey = "items/demo/missing.jpg";
        when(storageService.getMetadata(storageKey)).thenThrow(new NoSuchFileException(storageKey));

        mockMvc.perform(get("/files/" + storageKey))
                .andExpect(status().isNotFound());

        verify(storageService).getMetadata(storageKey);
        verify(storageService, never()).load(storageKey);
    }
}

