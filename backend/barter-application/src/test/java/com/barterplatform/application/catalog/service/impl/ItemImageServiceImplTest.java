package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemImageServiceImplTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemImageRepository itemImageRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService storageService;
    @Mock private ItemImageMapper itemImageMapper;

    private ItemImageServiceImpl service;

    // A minimal valid JPEG header (3 magic bytes)
    private static final byte[] JPEG_BYTES = buildJpeg();
    // A minimal valid PNG header
    private static final byte[] PNG_BYTES  = {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,
                                               0x00,0x00,0x00,0x0D,'I','H','D','R'};
    // text bytes (not a valid image)
    private static final byte[] FAKE_BYTES = "This is totally not an image".getBytes();

    private static byte[] buildJpeg() {
        byte[] b = new byte[20];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    @BeforeEach
    void setUp() {
        service = new ItemImageServiceImpl(
                itemRepository, itemImageRepository, userRepository,
                storageService, itemImageMapper,
                6,        // maxImagesPerItem
                5242880); // maxImageSizeBytes (5MB)
    }

    // ── Helpers ─────────────────────────────────────────────────

    private UserEntity user(Long id, UUID uuid) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setUuid(uuid);
        u.setUsername("user_" + id);
        u.setEmail("user" + id + "@test.com");
        u.setCreatedAt(OffsetDateTime.now());
        return u;
    }

    private ItemEntity item(UUID uuid, ItemStatus status) {
        ItemEntity e = new ItemEntity();
        e.setId(10L);
        e.setUuid(uuid);
        e.setOwnerId(1L);
        e.setCategoryId(10L);
        e.setTitle("Test Item");
        e.setStatus(status);
        e.setCondition(ItemCondition.GOOD);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private ItemImageEntity image(Long id, UUID uuid, int sortOrder, boolean primary) {
        ItemImageEntity e = new ItemImageEntity();
        e.setId(id);
        e.setUuid(uuid);
        e.setItemId(10L);
        e.setStorageKey("items/test/" + uuid + ".jpg");
        e.setOriginalFilename("test.jpg");
        e.setContentType("image/jpeg");
        e.setFileSize(1000L);
        e.setSortOrder(sortOrder);
        e.setPrimary(primary);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private ItemImageResponse response(UUID uuid, boolean primary) {
        ItemImageResponse r = new ItemImageResponse();
        r.setUuid(uuid);
        r.setIsPrimary(primary);
        r.setUrl("/files/items/test/" + uuid + ".jpg");
        return r;
    }

    private MultipartFile jpegFile(String name) {
        return new MockMultipartFile(name, name, "image/jpeg", JPEG_BYTES);
    }

    private MultipartFile pngFile() {
        return new MockMultipartFile("photo2.png", "photo2.png", "image/png", PNG_BYTES);
    }

    private MultipartFile fakeFile() {
        return new MockMultipartFile("fake.jpg", "fake.jpg", "image/jpeg", FAKE_BYTES);
    }

    // ══════════════════════════════════════════════════════════════
    //  uploadImage
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadImage")
    class UploadImage {

        @Test
        @DisplayName("first uploaded image becomes primary")
        void firstImageIsPrimary() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.countByItemId(10L)).thenReturn(0L);
            when(itemImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(itemImageMapper.toResponse(any())).thenReturn(response(UUID.randomUUID(), true));

            ItemImageResponse result = service.uploadImage(ownerUuid, itemUuid, jpegFile("photo.jpg"));

            assertTrue(result.getIsPrimary());

            ArgumentCaptor<ItemImageEntity> captor = ArgumentCaptor.forClass(ItemImageEntity.class);
            verify(itemImageRepository).save(captor.capture());
            assertTrue(captor.getValue().isPrimary(), "First image should be primary");
            assertEquals(0, captor.getValue().getSortOrder());
        }

        @Test
        @DisplayName("second uploaded image does NOT become primary")
        void secondImageIsNotPrimary() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.countByItemId(10L)).thenReturn(1L);
            when(itemImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UUID imageUuid = UUID.randomUUID();
            when(itemImageMapper.toResponse(any())).thenReturn(response(imageUuid, false));

            ItemImageResponse result = service.uploadImage(ownerUuid, itemUuid, pngFile());

            assertFalse(result.getIsPrimary());

            ArgumentCaptor<ItemImageEntity> captor = ArgumentCaptor.forClass(ItemImageEntity.class);
            verify(itemImageRepository).save(captor.capture());
            assertFalse(captor.getValue().isPrimary(), "Second image should not be primary");
            assertEquals(1, captor.getValue().getSortOrder());
        }

        @Test
        @DisplayName("throws FORBIDDEN when non-owner tries to upload")
        void nonOwnerForbidden() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity attacker = user(2L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid,  /* different owner */ ItemStatus.DRAFT);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(attacker));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.uploadImage(ownerUuid, itemUuid, jpegFile("x.jpg")));
            assertEquals(403, ex.getStatus().value());
            verify(itemImageRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when max images exceeded")
        void maxImagesExceeded() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.countByItemId(10L)).thenReturn(6L);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.uploadImage(ownerUuid, itemUuid, jpegFile("x.jpg")));
            assertEquals(400, ex.getStatus().value());
        }

        @Test
        @DisplayName("rejects fake content-type via magic byte validation")
        void fakeContentTypeRejected() throws IOException {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.countByItemId(10L)).thenReturn(0L);

            // File claims to be JPEG but bytes are "This is totally not an image"
            ApiException ex = assertThrows(ApiException.class,
                    () -> service.uploadImage(ownerUuid, itemUuid, fakeFile()));
            assertEquals(400, ex.getStatus().value());
            verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when item status is ARCHIVED")
        void archivedItemRejected() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ARCHIVED);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.uploadImage(ownerUuid, itemUuid, jpegFile("x.jpg")));
            assertEquals(400, ex.getStatus().value());
        }

        @Test
        @DisplayName("compensates by deleting file if DB save fails")
        void compensatesOnDbFailure() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.countByItemId(10L)).thenReturn(0L);
            when(itemImageRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class,
                    () -> service.uploadImage(ownerUuid, itemUuid, jpegFile("x.jpg")));

            // Storage should be asked to delete the saved file
            verify(storageService).delete(anyString());
        }

        @Test
        @DisplayName("returns clean API error when storage upload fails")
        void storageUploadFailureReturnsCleanApiError() throws Exception {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);
            String secret = "super-secret-account-key";

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.countByItemId(10L)).thenReturn(0L);
            doThrow(new IOException("Upload failed AccountKey=" + secret))
                    .when(storageService)
                    .store(anyString(), any(), anyLong(), anyString());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.uploadImage(ownerUuid, itemUuid, jpegFile("x.jpg")));

            assertEquals(500, ex.getStatus().value());
            assertEquals("Image storage is currently unavailable. Please try again later.", ex.getMessage());
            assertFalse(ex.getMessage().contains(secret));
            verify(itemImageRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  listImages
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listImages")
    class ListImages {

        @Test
        @DisplayName("returns images sorted by sortOrder")
        void returnsSortedImages() {
            UUID itemUuid = UUID.randomUUID();
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);
            UUID imgUuid1 = UUID.randomUUID();
            UUID imgUuid2 = UUID.randomUUID();
            ItemImageEntity img1 = image(1L, imgUuid1, 0, true);
            ItemImageEntity img2 = image(2L, imgUuid2, 1, false);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(itemImageRepository.findByItemIdOrderBySortOrderAsc(10L))
                    .thenReturn(List.of(img1, img2));
            when(itemImageMapper.toResponseList(any())).thenReturn(
                    List.of(response(imgUuid1, true), response(imgUuid2, false)));

            List<ItemImageResponse> result = service.listImages(itemUuid, null, false);

            assertEquals(2, result.size());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  deleteImage
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteImage")
    class DeleteImage {

        @Test
        @DisplayName("deletes non-primary image without promoting")
        void deleteNonPrimaryImage() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID imageUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);
            ItemImageEntity img = image(1L, imageUuid, 1, false);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.findByItemIdAndUuid(10L, imageUuid)).thenReturn(Optional.of(img));

            service.deleteImage(ownerUuid, itemUuid, imageUuid);

            verify(itemImageRepository).delete(img);
            verify(storageService).delete(img.getStorageKey());
            // No promote since image was not primary
            verify(itemImageRepository, never()).findFirstByItemIdOrderBySortOrderAsc(anyLong());
        }

        @Test
        @DisplayName("deletes primary image and promotes next image")
        void deletePrimaryImagePromotesNext() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID primaryUuid = UUID.randomUUID();
            UUID nextUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);
            ItemImageEntity primaryImg = image(1L, primaryUuid, 0, true);
            ItemImageEntity nextImg = image(2L, nextUuid, 1, false);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.findByItemIdAndUuid(10L, primaryUuid)).thenReturn(Optional.of(primaryImg));
            when(itemImageRepository.findFirstByItemIdOrderBySortOrderAsc(10L))
                    .thenReturn(Optional.of(nextImg));
            when(itemImageRepository.save(nextImg)).thenReturn(nextImg);

            service.deleteImage(ownerUuid, itemUuid, primaryUuid);

            verify(itemImageRepository).delete(primaryImg);
            // Next image should be saved as primary
            ArgumentCaptor<ItemImageEntity> captor = ArgumentCaptor.forClass(ItemImageEntity.class);
            verify(itemImageRepository).save(captor.capture());
            assertTrue(captor.getValue().isPrimary());
            assertEquals(nextUuid, captor.getValue().getUuid());
        }

        @Test
        @DisplayName("throws FORBIDDEN when non-owner tries to delete")
        void nonOwnerForbidden() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID imageUuid = UUID.randomUUID();
            UserEntity attacker = user(2L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(attacker));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.deleteImage(ownerUuid, itemUuid, imageUuid));
            assertEquals(403, ex.getStatus().value());
            verify(itemImageRepository, never()).delete(any());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  setPrimaryImage
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("setPrimaryImage")
    class SetPrimaryImage {

        @Test
        @DisplayName("sets requested image as primary and clears old primary")
        void success() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID imageUuid = UUID.randomUUID();
            UserEntity owner = user(1L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);
            ItemImageEntity img = image(2L, imageUuid, 1, false);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
            when(itemImageRepository.findByItemIdAndUuid(10L, imageUuid)).thenReturn(Optional.of(img));
            when(itemImageRepository.save(img)).thenReturn(img);
            when(itemImageMapper.toResponse(img)).thenReturn(response(imageUuid, true));

            ItemImageResponse result = service.setPrimaryImage(ownerUuid, itemUuid, imageUuid);

            assertTrue(result.getIsPrimary());
            verify(itemImageRepository).clearPrimaryForItem(10L);

            ArgumentCaptor<ItemImageEntity> captor = ArgumentCaptor.forClass(ItemImageEntity.class);
            verify(itemImageRepository).save(captor.capture());
            assertTrue(captor.getValue().isPrimary());
        }

        @Test
        @DisplayName("throws FORBIDDEN when non-owner tries to set primary")
        void nonOwnerForbidden() {
            UUID ownerUuid = UUID.randomUUID();
            UUID itemUuid = UUID.randomUUID();
            UUID imageUuid = UUID.randomUUID();
            UserEntity attacker = user(2L, ownerUuid);
            ItemEntity itemEntity = item(itemUuid, ItemStatus.ACTIVE);

            when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
            when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(attacker));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.setPrimaryImage(ownerUuid, itemUuid, imageUuid));
            assertEquals(403, ex.getStatus().value());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Invalid MIME type
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("rejects invalid MIME type (GIF bytes)")
    void invalidMimeTypeRejected() {
        UUID ownerUuid = UUID.randomUUID();
        UUID itemUuid = UUID.randomUUID();
        UserEntity owner = user(1L, ownerUuid);
        ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);

        when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
        when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
        when(itemImageRepository.countByItemId(10L)).thenReturn(0L);

        // GIF89a header
        byte[] gifBytes = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        MultipartFile gifFile = new MockMultipartFile("file", "anim.gif", "image/gif", gifBytes);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.uploadImage(ownerUuid, itemUuid, gifFile));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("cannot upload more than 6 images")
    void cannotUploadMoreThan6Images() {
        UUID ownerUuid = UUID.randomUUID();
        UUID itemUuid = UUID.randomUUID();
        UserEntity owner = user(1L, ownerUuid);
        ItemEntity itemEntity = item(itemUuid, ItemStatus.DRAFT);

        when(itemRepository.findByUuid(itemUuid)).thenReturn(Optional.of(itemEntity));
        when(userRepository.findByUuid(ownerUuid)).thenReturn(Optional.of(owner));
        when(itemImageRepository.countByItemId(10L)).thenReturn(6L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.uploadImage(ownerUuid, itemUuid, jpegFile("x.jpg")));
        assertEquals(400, ex.getStatus().value());
    }
}

