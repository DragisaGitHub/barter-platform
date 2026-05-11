package com.barterplatform.application.catalog.mapper;

import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItemImageMapper {

    private final FileStorageService storageService;

    public ItemImageMapper(FileStorageService storageService) {
        this.storageService = storageService;
    }

    public ItemImageResponse toResponse(ItemImageEntity entity) {
        ItemImageResponse r = new ItemImageResponse();
        r.setUuid(entity.getUuid());
        r.setUrl(storageService.resolveUrl(entity.getStorageKey()));
        r.setOriginalFilename(entity.getOriginalFilename());
        r.setContentType(entity.getContentType());
        r.setFileSize(entity.getFileSize());
        r.setSortOrder(entity.getSortOrder());
        r.setIsPrimary(entity.isPrimary());
        r.setCreatedAt(entity.getCreatedAt());
        return r;
    }

    public List<ItemImageResponse> toResponseList(List<ItemImageEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }
}

