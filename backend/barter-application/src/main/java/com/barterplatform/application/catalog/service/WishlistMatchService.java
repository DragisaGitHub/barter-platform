package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.WishlistMatchResponse;
import java.util.List;
import java.util.UUID;

public interface WishlistMatchService {

    List<WishlistMatchResponse> listWishlistMatches(
            UUID currentUserUuid,
            UUID wishlistItemUuid);
}