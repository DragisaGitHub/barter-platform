package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.service.ItemImageService;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ItemImageServiceImpl implements ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageServiceImpl.class);

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    // Magic bytes for JPEG, PNG, and WebP
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP_RIFF  = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_SIG   = {0x57, 0x45, 0x42, 0x50}; // "WEBP" at offset 8

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;
    private final ItemImageMapper itemImageMapper;

    private final int maxImagesPerItem;
    private final long maxImageSizeBytes;

    public ItemImageServiceImpl(
            ItemRepository itemRepository,
            ItemImageRepository itemImageRepository,
            UserRepository userRepository,
            FileStorageService storageService,
            ItemImageMapper itemImageMapper,
            @Value("${barter.storage.max-images-per-item:6}") int maxImagesPerItem,
            @Value("${barter.storage.max-image-size-bytes:5242880}") long maxImageSizeBytes) {
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.itemImageMapper = itemImageMapper;
        this.maxImagesPerItem = maxImagesPerItem;
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    // ── uploadImage ────────────────────────────────────────────────

    @Override
    public ItemImageResponse uploadImage(UUID currentUserUuid, UUID itemUuid, MultipartFile file) {
        ItemEntity item = resolveItem(itemUuid);
        UserEntity owner = resolveUser(currentUserUuid);
        enforceOwnership(item, owner);
        enforceEditableStatus(item);

        validateFile(file);

        long imageCount = itemImageRepository.countByItemId(item.getId());
        if (imageCount >= maxImagesPerItem) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                    "Item already has the maximum number of images (%d).".formatted(maxImagesPerItem));
        }

        String detectedContentType = detectContentType(file);
        String extension = ALLOWED_CONTENT_TYPES.get(detectedContentType);
        UUID imageUuid = UUID.randomUUID();

        // storage key: items/{itemUuid}/{imageUuid}.{ext}
        String storageKey = "items/%s/%s.%s".formatted(itemUuid, imageUuid, extension);

        String storageKeySaved = null;
        try {
            try (InputStream is = file.getInputStream()) {
                storageService.store(storageKey, is, detectedContentType);
                storageKeySaved = storageKey;
            }

            ItemImageEntity image = new ItemImageEntity();
            image.setUuid(imageUuid);
            image.setItemId(item.getId());
            image.setStorageKey(storageKey);
            image.setOriginalFilename(sanitizeFilename(file.getOriginalFilename()));
            image.setContentType(detectedContentType);
            image.setFileSize(file.getSize());
            image.setSortOrder((int) imageCount);
            image.setPrimary(imageCount == 0); // first image becomes primary

            ItemImageEntity saved = itemImageRepository.save(image);
            return itemImageMapper.toResponse(saved);

        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                    "Failed to store image file.", e);
        } catch (Exception e) {
            // Compensate: if DB save fails after file was stored, remove the file
            if (storageKeySaved != null) {
                storageService.delete(storageKeySaved);
                log.warn("Rolled back stored file '{}' after DB save failure: {}", storageKeySaved, e.getMessage());
            }
            throw e;
        }
    }

    // ── listImages ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ItemImageResponse> listImages(UUID itemUuid) {
        ItemEntity item = resolveItem(itemUuid);
        List<ItemImageEntity> images = itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId());
        return itemImageMapper.toResponseList(images);
    }

    // ── deleteImage ────────────────────────────────────────────────

    @Override
    public void deleteImage(UUID currentUserUuid, UUID itemUuid, UUID imageUuid) {
        ItemEntity item = resolveItem(itemUuid);
        UserEntity owner = resolveUser(currentUserUuid);
        enforceOwnership(item, owner);
        enforceEditableStatus(item);

        ItemImageEntity image = itemImageRepository.findByItemIdAndUuid(item.getId(), imageUuid)
                .orElseThrow(() -> notFound("Image with uuid '%s' was not found for item '%s'.", imageUuid, itemUuid));

        boolean wasPrimary = image.isPrimary();
        String storageKey = image.getStorageKey();

        itemImageRepository.delete(image);
        itemImageRepository.flush();

        // If deleted image was primary, promote the next image by sortOrder
        if (wasPrimary) {
            itemImageRepository.findFirstByItemIdOrderBySortOrderAsc(item.getId())
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        itemImageRepository.save(next);
                    });
        }

        storageService.delete(storageKey);
    }

    // ── setPrimaryImage ────────────────────────────────────────────

    @Override
    public ItemImageResponse setPrimaryImage(UUID currentUserUuid, UUID itemUuid, UUID imageUuid) {
        ItemEntity item = resolveItem(itemUuid);
        UserEntity owner = resolveUser(currentUserUuid);
        enforceOwnership(item, owner);
        enforceEditableStatus(item);

        ItemImageEntity image = itemImageRepository.findByItemIdAndUuid(item.getId(), imageUuid)
                .orElseThrow(() -> notFound("Image with uuid '%s' was not found for item '%s'.", imageUuid, itemUuid));

        // Clear existing primary
        itemImageRepository.clearPrimaryForItem(item.getId());

        image.setPrimary(true);
        ItemImageEntity saved = itemImageRepository.save(image);
        return itemImageMapper.toResponse(saved);
    }

    // ── Validation helpers ─────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "File must not be empty.");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                    "File size exceeds maximum allowed size of %d bytes.".formatted(maxImageSizeBytes));
        }
    }

    /**
     * Detect content type by reading magic bytes from the file.
     * Returns the validated content type string.
     */
    private String detectContentType(MultipartFile file) {
        byte[] header;
        try {
            header = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Cannot read file content.");
        }

        if (startsWith(header, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        if (startsWith(header, PNG_MAGIC)) {
            return "image/png";
        }
        if (header.length >= 12 && startsWith(header, WEBP_RIFF)
                && startsWith(header, 8, WEBP_SIG)) {
            return "image/webp";
        }

        // Report what we found (or what was claimed)
        String claimed = file.getContentType();
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                "Unsupported or unrecognized image format. Allowed types: JPEG, PNG, WebP. Claimed: " + claimed);
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        return startsWith(data, 0, prefix);
    }

    private boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "upload";
        }
        // Remove path separators and problematic characters
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }

    // ── Domain helpers ─────────────────────────────────────────────

    private ItemEntity resolveItem(UUID itemUuid) {
        ItemEntity item = itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));
        if (item.getDeletedAt() != null || item.getStatus() == ItemStatus.REMOVED) {
            throw notFound("Item with uuid '%s' was not found.", itemUuid);
        }
        return item;
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private void enforceOwnership(ItemEntity item, UserEntity user) {
        if (!item.getOwnerId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                    "You are not the owner of this item.");
        }
    }

    private void enforceEditableStatus(ItemEntity item) {
        ItemStatus status = item.getStatus();
        if (status != ItemStatus.DRAFT && status != ItemStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                    "Images can only be modified when item status is DRAFT or ACTIVE.");
        }
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

