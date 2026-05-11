package com.barterplatform.application.trade.mapper;

import com.barterplatform.api.model.TradeOfferItemSummary;
import com.barterplatform.api.model.TradeOfferResponse;
import com.barterplatform.api.model.TradeOfferSummaryResponse;
import com.barterplatform.api.model.TradeOfferUserSummary;
import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface TradeOfferMapper {

    // ── Enum mappings (domain → API) ────────────────────────────

    default com.barterplatform.api.model.TradeOfferStatus map(
            com.barterplatform.domain.trade.enums.TradeOfferStatus status) {
        return status == null ? null
                : com.barterplatform.api.model.TradeOfferStatus.valueOf(status.name());
    }

    default com.barterplatform.api.model.TradeOfferMode map(TradeOfferMode mode) {
        return mode == null ? null
                : com.barterplatform.api.model.TradeOfferMode.valueOf(mode.name());
    }

    default com.barterplatform.api.model.ItemStatus map(
            com.barterplatform.domain.catalog.enums.ItemStatus status) {
        return status == null ? null
                : com.barterplatform.api.model.ItemStatus.valueOf(status.name());
    }

    default com.barterplatform.api.model.ItemCondition map(
            com.barterplatform.domain.catalog.enums.ItemCondition condition) {
        return condition == null ? null
                : com.barterplatform.api.model.ItemCondition.valueOf(condition.name());
    }

    // ── TradeOfferUserSummary ────────────────────────────────────

    TradeOfferUserSummary toUserSummary(UserEntity user);

    // ── TradeOfferItemSummary ────────────────────────────────────

    @Mapping(target = "categoryName", ignore = true)
    TradeOfferItemSummary toItemSummary(ItemEntity item);

    default TradeOfferItemSummary toItemSummary(ItemEntity item,
                                                CategoryEntity category) {
        TradeOfferItemSummary summary = toItemSummary(item);
        summary.setCategoryName(category.getName());
        return summary;
    }

    // ── TradeOfferSummaryResponse ────────────────────────────────

    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "senderItem", ignore = true)
    @Mapping(target = "receiverItem", ignore = true)
    @Mapping(target = "offeredItems", ignore = true)
    TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity entity);

    default TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity entity,
                                                         UserEntity senderUser,
                                                         UserEntity receiverUser,
                                                         ItemEntity receiverItem,
                                                         CategoryEntity receiverCategory,
                                                         List<ItemEntity> offeredItemEntities,
                                                         List<CategoryEntity> offeredCategories) {
        TradeOfferSummaryResponse response = toSummaryResponse(entity);
        response.setSender(toUserSummary(senderUser));
        response.setReceiver(toUserSummary(receiverUser));
        response.setReceiverItem(toItemSummary(receiverItem, receiverCategory));

        List<TradeOfferItemSummary> offeredSummaries = buildOfferedItems(offeredItemEntities, offeredCategories);
        response.setOfferedItems(offeredSummaries);
        // Backward compat: senderItem = first offered item or null
        response.setSenderItem(offeredSummaries.isEmpty() ? null : offeredSummaries.getFirst());

        return response;
    }

    // ── TradeOfferResponse ──────────────────────────────────────

    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "senderItem", ignore = true)
    @Mapping(target = "receiverItem", ignore = true)
    @Mapping(target = "offeredItems", ignore = true)
    TradeOfferResponse toResponse(TradeOfferEntity entity);

    default TradeOfferResponse toResponse(TradeOfferEntity entity,
                                           UserEntity senderUser,
                                           UserEntity receiverUser,
                                           ItemEntity receiverItem,
                                           CategoryEntity receiverCategory,
                                           List<ItemEntity> offeredItemEntities,
                                           List<CategoryEntity> offeredCategories) {
        TradeOfferResponse response = toResponse(entity);
        response.setSender(toUserSummary(senderUser));
        response.setReceiver(toUserSummary(receiverUser));
        response.setReceiverItem(toItemSummary(receiverItem, receiverCategory));

        List<TradeOfferItemSummary> offeredSummaries = buildOfferedItems(offeredItemEntities, offeredCategories);
        response.setOfferedItems(offeredSummaries);
        // Backward compat: senderItem = first offered item or null
        response.setSenderItem(offeredSummaries.isEmpty() ? null : offeredSummaries.getFirst());

        return response;
    }

    // ── Private helper ──────────────────────────────────────────

    private List<TradeOfferItemSummary> buildOfferedItems(List<ItemEntity> items,
                                                          List<CategoryEntity> categories) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return java.util.stream.IntStream.range(0, items.size())
                .mapToObj(i -> toItemSummary(items.get(i), categories.get(i)))
                .toList();
    }
}

