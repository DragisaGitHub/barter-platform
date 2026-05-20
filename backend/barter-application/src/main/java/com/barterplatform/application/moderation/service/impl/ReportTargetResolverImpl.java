package com.barterplatform.application.moderation.service.impl;

import com.barterplatform.application.moderation.service.ReportTargetResolver;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.domain.moderation.report.ReportTargetType;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferMessageEntity;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferMessageRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReportTargetResolverImpl implements ReportTargetResolver {

    private static final int PREVIEW_LIMIT = 180;

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final TradeOfferMessageRepository tradeOfferMessageRepository;
    private final TradeReviewRepository tradeReviewRepository;

    public ReportTargetResolverImpl(
            ItemRepository itemRepository,
            UserRepository userRepository,
            TradeOfferRepository tradeOfferRepository,
            TradeOfferMessageRepository tradeOfferMessageRepository,
            TradeReviewRepository tradeReviewRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeOfferMessageRepository = tradeOfferMessageRepository;
        this.tradeReviewRepository = tradeReviewRepository;
    }

    @Override
    public void validateForCreate(ReportTargetType targetType, UUID targetUuid, UserEntity reporter) {
        switch (targetType) {
            case ITEM -> validateItem(targetUuid, reporter);
            case USER -> validateUser(targetUuid, reporter);
            case MESSAGE -> validateMessage(targetUuid, reporter);
            case TRADE_OFFER -> validateTradeOffer(targetUuid, reporter);
            case REVIEW -> validateReview(targetUuid, reporter);
        }
    }

    @Override
    public TargetSummary resolveSummary(ReportTargetType targetType, UUID targetUuid) {
        return switch (targetType) {
            case ITEM -> resolveItemSummary(targetUuid);
            case USER -> resolveUserSummary(targetUuid);
            case MESSAGE -> resolveMessageSummary(targetUuid);
            case TRADE_OFFER -> resolveTradeOfferSummary(targetUuid);
            case REVIEW -> resolveReviewSummary(targetUuid);
        };
    }

    private void validateItem(UUID targetUuid, UserEntity reporter) {
        ItemEntity item = resolveReportableItem(targetUuid);
        if (item.getOwnerId().equals(reporter.getId())) {
            throw forbidden("You cannot report your own listing.");
        }
    }

    private void validateUser(UUID targetUuid, UserEntity reporter) {
        UserEntity user = resolveActiveUser(targetUuid);
        if (user.getId().equals(reporter.getId())) {
            throw forbidden("You cannot report your own profile.");
        }
    }

    private void validateMessage(UUID targetUuid, UserEntity reporter) {
        TradeOfferMessageEntity message = resolveMessage(targetUuid);
        TradeOfferEntity offer = resolveTradeOffer(message.getTradeOfferId());
        validateParticipant(reporter, offer, "You are not a participant in this trade offer.");
        if (message.getSenderUserId().equals(reporter.getId())) {
            throw forbidden("You cannot report your own message.");
        }
    }

    private void validateTradeOffer(UUID targetUuid, UserEntity reporter) {
        TradeOfferEntity offer = resolveTradeOffer(targetUuid);
        validateParticipant(reporter, offer, "You are not a participant in this trade offer.");
    }

    private void validateReview(UUID targetUuid, UserEntity reporter) {
        TradeReviewEntity review = resolveReview(targetUuid);
        if (review.getReviewerUserId().equals(reporter.getId())) {
            throw forbidden("You cannot report your own review.");
        }
        TradeOfferEntity offer = resolveTradeOffer(review.getTradeOfferId());
        validateParticipant(reporter, offer, "You are not a participant in this trade review.");
    }

    private TargetSummary resolveItemSummary(UUID targetUuid) {
        return itemRepository.findByUuid(targetUuid)
                .filter(item -> item.getDeletedAt() == null)
                .map(item -> {
                    String ownerName = userRepository.findById(item.getOwnerId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown user");
                    return new TargetSummary(
                            item.getTitle(),
                            "Listing by " + ownerName,
                            truncate(item.getDescription()));
                })
                .orElseGet(() -> unavailableTarget(targetUuid));
    }

    private TargetSummary resolveUserSummary(UUID targetUuid) {
        return userRepository.findByUuid(targetUuid)
                .map(user -> new TargetSummary(
                        user.getUsername(),
                        "Public profile",
                        user.getStatus().name()))
                .orElseGet(() -> unavailableTarget(targetUuid));
    }

    private TargetSummary resolveMessageSummary(UUID targetUuid) {
        return tradeOfferMessageRepository.findByUuid(targetUuid)
                .map(message -> {
                    String sender = userRepository.findById(message.getSenderUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown sender");
                    String recipient = userRepository.findById(message.getRecipientUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown recipient");
                    return new TargetSummary(
                            "Trade message",
                            sender + " → " + recipient,
                            truncate(message.getContent()));
                })
                .orElseGet(() -> unavailableTarget(targetUuid));
    }

    private TargetSummary resolveTradeOfferSummary(UUID targetUuid) {
        return tradeOfferRepository.findByUuid(targetUuid)
                .map(offer -> {
                    String sender = userRepository.findById(offer.getSenderUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown sender");
                    String receiver = userRepository.findById(offer.getReceiverUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown receiver");
                    String requestedItemTitle = itemRepository.findById(offer.getReceiverItemId())
                            .map(ItemEntity::getTitle)
                            .orElse("Unknown listing");
                    String preview = offer.getMessage() == null || offer.getMessage().isBlank()
                            ? "Requested listing: " + requestedItemTitle
                            : truncate(offer.getMessage());
                    return new TargetSummary(
                            "Trade offer",
                            sender + " ↔ " + receiver,
                            preview);
                })
                .orElseGet(() -> unavailableTarget(targetUuid));
    }

    private TargetSummary resolveReviewSummary(UUID targetUuid) {
        return tradeReviewRepository.findByUuid(targetUuid)
                .map(review -> {
                    String reviewer = userRepository.findById(review.getReviewerUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown reviewer");
                    String reviewed = userRepository.findById(review.getReviewedUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown user");
                    String preview = review.getComment() != null && !review.getComment().isBlank()
                            ? truncate(review.getComment())
                            : review.getRating().name() + (review.getNegativeReason() == null
                                    ? ""
                                    : " · " + review.getNegativeReason().name());
                    return new TargetSummary(
                            "Trade review",
                            reviewer + " → " + reviewed,
                            preview);
                })
                .orElseGet(() -> unavailableTarget(targetUuid));
    }

    private ItemEntity resolveReportableItem(UUID targetUuid) {
        ItemEntity item = itemRepository.findByUuid(targetUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.".formatted(targetUuid)));
        if (item.getDeletedAt() != null || item.getStatus() == ItemStatus.REMOVED) {
            throw notFound("Item with uuid '%s' was not found.".formatted(targetUuid));
        }
        return item;
    }

    private UserEntity resolveActiveUser(UUID targetUuid) {
        UserEntity user = userRepository.findByUuid(targetUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.".formatted(targetUuid)));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw notFound("User with uuid '%s' was not found.".formatted(targetUuid));
        }
        return user;
    }

    private TradeOfferMessageEntity resolveMessage(UUID targetUuid) {
        return tradeOfferMessageRepository.findByUuid(targetUuid)
                .orElseThrow(() -> notFound("Trade message with uuid '%s' was not found.".formatted(targetUuid)));
    }

    private TradeReviewEntity resolveReview(UUID targetUuid) {
        return tradeReviewRepository.findByUuid(targetUuid)
                .orElseThrow(() -> notFound("Trade review with uuid '%s' was not found.".formatted(targetUuid)));
    }

    private TradeOfferEntity resolveTradeOffer(UUID targetUuid) {
        return tradeOfferRepository.findByUuid(targetUuid)
                .orElseThrow(() -> notFound("Trade offer with uuid '%s' was not found.".formatted(targetUuid)));
    }

    private TradeOfferEntity resolveTradeOffer(Long tradeOfferId) {
        return tradeOfferRepository.findById(tradeOfferId)
                .orElseThrow(() -> notFound("Trade offer was not found."));
    }

    private void validateParticipant(UserEntity reporter, TradeOfferEntity offer, String message) {
        boolean participant = offer.getSenderUserId().equals(reporter.getId())
                || offer.getReceiverUserId().equals(reporter.getId());
        if (!participant) {
            throw forbidden(message);
        }
    }

    private TargetSummary unavailableTarget(UUID targetUuid) {
        return new TargetSummary(
                "Unavailable target",
                targetUuid.toString(),
                "The reported content is no longer available.");
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() <= PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LIMIT - 1) + "…";
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, message);
    }
}

