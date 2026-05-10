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
    TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity entity);

    default TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity entity,
                                                         UserEntity senderUser,
                                                         UserEntity receiverUser,
                                                         ItemEntity senderItem,
                                                         CategoryEntity senderCategory,
                                                         ItemEntity receiverItem,
                                                         CategoryEntity receiverCategory) {
        TradeOfferSummaryResponse response = toSummaryResponse(entity);
        response.setSender(toUserSummary(senderUser));
        response.setReceiver(toUserSummary(receiverUser));
        response.setSenderItem(toItemSummary(senderItem, senderCategory));
        response.setReceiverItem(toItemSummary(receiverItem, receiverCategory));
        return response;
    }

    // ── TradeOfferResponse ──────────────────────────────────────

    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "senderItem", ignore = true)
    @Mapping(target = "receiverItem", ignore = true)
    TradeOfferResponse toResponse(TradeOfferEntity entity);

    default TradeOfferResponse toResponse(TradeOfferEntity entity,
                                           UserEntity senderUser,
                                           UserEntity receiverUser,
                                           ItemEntity senderItem,
                                           CategoryEntity senderCategory,
                                           ItemEntity receiverItem,
                                           CategoryEntity receiverCategory) {
        TradeOfferResponse response = toResponse(entity);
        response.setSender(toUserSummary(senderUser));
        response.setReceiver(toUserSummary(receiverUser));
        response.setSenderItem(toItemSummary(senderItem, senderCategory));
        response.setReceiverItem(toItemSummary(receiverItem, receiverCategory));
        return response;
    }
}

