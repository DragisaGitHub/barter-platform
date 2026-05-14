package com.barterplatform.infrastructure.catalog.repository;

import java.util.UUID;

public interface PopularCategoryProjection {

    UUID getUuid();

    String getName();

    String getSlug();

    String getDescription();

    Integer getSortOrder();

    Long getActiveItemCount();
}

