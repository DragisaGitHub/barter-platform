package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.MessageResponse;
import java.util.UUID;

public interface FavoriteItemService {

    MessageResponse favoriteItem(UUID currentUserUuid, UUID itemUuid);

    void unfavoriteItem(UUID currentUserUuid, UUID itemUuid);

    ItemPagedResponse listFavoriteItems(UUID currentUserUuid, Integer page, Integer size, String sort);
}

