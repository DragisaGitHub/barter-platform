package com.barterplatform.application.catalog.mapper;

import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.api.model.ItemDetailResponse;
import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface ItemMapper {

    // ── Enum mappings (domain → API) ────────────────────────────

    default com.barterplatform.api.model.ItemStatus map(
            com.barterplatform.domain.catalog.enums.ItemStatus status) {
        return status == null ? null : com.barterplatform.api.model.ItemStatus.valueOf(status.name());
    }

    default com.barterplatform.api.model.ItemCondition map(
            com.barterplatform.domain.catalog.enums.ItemCondition condition) {
        return condition == null ? null : com.barterplatform.api.model.ItemCondition.valueOf(condition.name());
    }

    // ── ItemSummaryResponse ─────────────────────────────────────

    @Mapping(target = "categoryUuid", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "ownerUuid", ignore = true)
    @Mapping(target = "ownerUsername", ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    ItemSummaryResponse toSummaryResponse(ItemEntity entity);

    default ItemSummaryResponse toSummaryResponse(ItemEntity entity,
                                                   CategoryEntity category,
                                                   UUID ownerUuid,
                                                   String ownerUsername) {
        return toSummaryResponse(entity, category, ownerUuid, ownerUsername, null);
    }

    default ItemSummaryResponse toSummaryResponse(ItemEntity entity,
                                                   CategoryEntity category,
                                                   UUID ownerUuid,
                                                   String ownerUsername,
                                                   String primaryImageUrl) {
        ItemSummaryResponse response = toSummaryResponse(entity);
        response.setCategoryUuid(category.getUuid());
        response.setCategoryName(category.getName());
        response.setOwnerUuid(ownerUuid);
        response.setOwnerUsername(ownerUsername);
        response.setPrimaryImageUrl(primaryImageUrl);
        return response;
    }

    // ── ItemDetailResponse ──────────────────────────────────────

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "ownerUuid", ignore = true)
    @Mapping(target = "ownerUsername", ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    ItemDetailResponse toDetailResponse(ItemEntity entity);

    default ItemDetailResponse toDetailResponse(ItemEntity entity,
                                                 CategoryEntity category,
                                                 List<TagEntity> tags,
                                                 UUID ownerUuid,
                                                 String ownerUsername) {
        return toDetailResponse(entity, category, tags, ownerUuid, ownerUsername, null, List.of());
    }

    default ItemDetailResponse toDetailResponse(ItemEntity entity,
                                                 CategoryEntity category,
                                                 List<TagEntity> tags,
                                                 UUID ownerUuid,
                                                 String ownerUsername,
                                                 String primaryImageUrl,
                                                 List<ItemImageResponse> images) {
        ItemDetailResponse response = toDetailResponse(entity);

        // Map category
        CategoryResponse catResponse = new CategoryResponse();
        catResponse.setUuid(category.getUuid());
        catResponse.setName(category.getName());
        catResponse.setSlug(category.getSlug());
        catResponse.setDescription(category.getDescription());
        catResponse.setSortOrder(category.getSortOrder());
        response.setCategory(catResponse);

        // Map tags
        List<TagResponse> tagResponses = new ArrayList<>();
        if (tags != null) {
            for (TagEntity tag : tags) {
                TagResponse tr = new TagResponse();
                tr.setUuid(tag.getUuid());
                tr.setName(tag.getName());
                tr.setSlug(tag.getSlug());
                tagResponses.add(tr);
            }
        }
        response.setTags(tagResponses);

        response.setOwnerUuid(ownerUuid);
        response.setOwnerUsername(ownerUsername);
        response.setPrimaryImageUrl(primaryImageUrl);
        response.setImages(images != null ? images : List.of());
        return response;
    }
}

