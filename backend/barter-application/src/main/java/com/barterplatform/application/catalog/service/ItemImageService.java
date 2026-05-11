package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.ItemImageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ItemImageService {

    ItemImageResponse uploadImage(UUID currentUserUuid, UUID itemUuid, MultipartFile file);

    List<ItemImageResponse> listImages(UUID itemUuid);

    void deleteImage(UUID currentUserUuid, UUID itemUuid, UUID imageUuid);

    ItemImageResponse setPrimaryImage(UUID currentUserUuid, UUID itemUuid, UUID imageUuid);
}

