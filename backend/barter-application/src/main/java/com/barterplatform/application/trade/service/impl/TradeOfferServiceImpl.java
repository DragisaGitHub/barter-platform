package com.barterplatform.application.trade.service.impl;

import com.barterplatform.api.model.CreateTradeOfferRequest;
import com.barterplatform.api.model.ItemListingEntryResponse;
import com.barterplatform.api.model.TradeOfferPagedResponse;
import com.barterplatform.api.model.TradeOfferResponse;
import com.barterplatform.api.model.TradeOfferSummaryResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.application.reputation.mapper.TradeReviewMapper;
import com.barterplatform.application.trade.mapper.TradeOfferMapper;
import com.barterplatform.application.trade.service.TradeOfferService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemListingEntryEntity;
import com.barterplatform.domain.catalog.enums.ListingMode;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.domain.reputation.entity.TradeReviewEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferItemEntity;
import com.barterplatform.domain.trade.entity.TradeOfferRequestedEntryEntity;
import com.barterplatform.domain.trade.enums.TradeOfferItemSide;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemListingEntryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferItemRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRequestedEntryRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeOfferServiceImpl implements TradeOfferService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "respondedAt");

    private final TradeOfferRepository tradeOfferRepository;
    private final TradeOfferItemRepository tradeOfferItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemListingEntryRepository itemListingEntryRepository;
    private final CategoryRepository categoryRepository;
    private final TradeOfferMapper tradeOfferMapper;
    private final TradeReviewRepository tradeReviewRepository;
    private final TradeReviewMapper tradeReviewMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;
    private final NotificationService notificationService;
    private final TradeOfferRequestedEntryRepository tradeOfferRequestedEntryRepository;

    public TradeOfferServiceImpl(TradeOfferRepository tradeOfferRepository,
                                 TradeOfferItemRepository tradeOfferItemRepository,
                                 UserRepository userRepository,
                                 ItemRepository itemRepository,
                                 ItemListingEntryRepository itemListingEntryRepository,
                                 CategoryRepository categoryRepository,
                                 TradeOfferMapper tradeOfferMapper,
                                 TradeReviewRepository tradeReviewRepository,
                                 TradeReviewMapper tradeReviewMapper,
                                 PageRequestFactory pageRequestFactory,
                                 PageResponseMapper pageResponseMapper,
                                 NotificationService notificationService,
                                 TradeOfferRequestedEntryRepository tradeOfferRequestedEntryRepository) {
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeOfferItemRepository = tradeOfferItemRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.itemListingEntryRepository = itemListingEntryRepository;
        this.categoryRepository = categoryRepository;
        this.tradeOfferMapper = tradeOfferMapper;
        this.tradeReviewRepository = tradeReviewRepository;
        this.tradeReviewMapper = tradeReviewMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
        this.notificationService = notificationService;
        this.tradeOfferRequestedEntryRepository = tradeOfferRequestedEntryRepository;
    }

    // ── Create ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse createOffer(UUID currentUserUuid, CreateTradeOfferRequest request) {
        UserEntity sender = resolveUser(currentUserUuid);

        // Resolve mode
        TradeOfferMode mode = TradeOfferMode.valueOf(request.getMode().name());
        List<UUID> senderItemUuids = request.getSenderItemUuids();

        // Validate mode-specific rules
        validateModeConstraints(mode, senderItemUuids, request.getMessage());

        // Resolve receiver item
        ItemEntity receiverItem = resolveItem(request.getReceiverItemUuid());
        List<ItemListingEntryEntity> requestedEntries = validateRequestedEntries(receiverItem, request.getRequestedEntryUuids());

        // Receiver item must NOT belong to sender (no self-offers)
        if (receiverItem.getOwnerId().equals(sender.getId())) {
            throw forbidden("Cannot create a trade offer for your own item.");
        }

        // Receiver item must be ACTIVE
        if (receiverItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Receiver item is not active.");
        }

        // Resolve and validate sender items
        List<ItemEntity> senderItems = resolveSenderItems(senderItemUuids, sender);

        // Derive receiver user from receiver item owner
        UserEntity receiver = userRepository.findById(receiverItem.getOwnerId())
                .orElseThrow(() -> notFound("Owner of receiver item was not found."));

        // Build and save the offer
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setSenderUserId(sender.getId());
        offer.setReceiverUserId(receiver.getId());
        offer.setSenderItemId(senderItems.isEmpty() ? null : senderItems.getFirst().getId());
        offer.setReceiverItemId(receiverItem.getId());
        offer.setMode(mode);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setMessage(request.getMessage());

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Create trade_offer_items entries
        for (ItemEntity senderItem : senderItems) {
            TradeOfferItemEntity toi = new TradeOfferItemEntity();
            toi.setTradeOffer(saved);
            toi.setItemId(senderItem.getId());
            toi.setSide(TradeOfferItemSide.OFFERED);
            saved.getItems().add(toi);
        }

        TradeOfferItemEntity requestedToi = new TradeOfferItemEntity();
        requestedToi.setTradeOffer(saved);
        requestedToi.setItemId(receiverItem.getId());
        requestedToi.setSide(TradeOfferItemSide.REQUESTED);
        saved.getItems().add(requestedToi);

        for (ItemListingEntryEntity requestedEntry : requestedEntries) {
            TradeOfferRequestedEntryEntity requestedEntryEntity = new TradeOfferRequestedEntryEntity();
            requestedEntryEntity.setTradeOffer(saved);
            requestedEntryEntity.setItemListingEntryId(requestedEntry.getId());
            tradeOfferRequestedEntryRepository.save(requestedEntryEntity);
        }

        tradeOfferRepository.save(saved);

        // Notify the receiver that a new trade offer was received
        String senderItemSummary = senderItems.isEmpty()
                ? ""
                : " for \"" + senderItems.getFirst().getTitle() + "\"";
        notificationService.createNotification(
                receiver.getId(),
                NotificationType.TRADE_OFFER_RECEIVED,
                "New trade offer from " + sender.getUsername(),
                sender.getUsername() + " wants \"" + receiverItem.getTitle() + "\"" + senderItemSummary + ".",
                saved.getUuid(),
                "TRADE_OFFER");

        return toResponse(saved, sender, receiver, senderItems, receiverItem, sender.getId());
    }

    // ── List Incoming ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TradeOfferPagedResponse listIncoming(UUID currentUserUuid, Integer page, Integer size,
                                                 String sort, TradeOfferStatus status) {
        UserEntity user = resolveUser(currentUserUuid);
        PageRequestFactory.ResolvedPageRequest resolved =
                pageRequestFactory.create(page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        Page<TradeOfferEntity> offers = (status != null)
                ? tradeOfferRepository.findByReceiverUserIdAndStatus(user.getId(), status, resolved.pageable())
                : tradeOfferRepository.findByReceiverUserId(user.getId(), resolved.pageable());

        List<TradeOfferSummaryResponse> content = offers.getContent().stream()
                .map(offer -> toSummaryResponse(offer, user.getId()))
                .toList();

        return pageResponseMapper.toTradeOfferPagedResponse(offers, content, resolved.sort());
    }

    // ── List Sent ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TradeOfferPagedResponse listSent(UUID currentUserUuid, Integer page, Integer size,
                                             String sort, TradeOfferStatus status) {
        UserEntity user = resolveUser(currentUserUuid);
        PageRequestFactory.ResolvedPageRequest resolved =
                pageRequestFactory.create(page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        Page<TradeOfferEntity> offers = (status != null)
                ? tradeOfferRepository.findBySenderUserIdAndStatus(user.getId(), status, resolved.pageable())
                : tradeOfferRepository.findBySenderUserId(user.getId(), resolved.pageable());

        List<TradeOfferSummaryResponse> content = offers.getContent().stream()
                .map(offer -> toSummaryResponse(offer, user.getId()))
                .toList();

        return pageResponseMapper.toTradeOfferPagedResponse(offers, content, resolved.sort());
    }

    // ── Get ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TradeOfferResponse getOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only sender or receiver can view
        if (!isParticipant(offer, user)) {
            throw forbidden("You are not a participant of this trade offer.");
        }

        return toFullResponse(offer, user.getId());
    }

    // ── Accept ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse acceptOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only receiver can accept
        if (!offer.getReceiverUserId().equals(user.getId())) {
            throw forbidden("Only the receiver can accept a trade offer.");
        }

        // Only PENDING can be accepted (domain method handles assertion)
        try {
            offer.accept();
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        // Receiver item must still be ACTIVE
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));
        if (receiverItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Receiver item is no longer active.");
        }

        // Resolve offered items from trade_offer_items
        List<Long> offeredItemIds = tradeOfferItemRepository.findItemIdsByTradeOfferIdAndSide(
                offer.getId(), TradeOfferItemSide.OFFERED);
        List<ItemEntity> offeredItems = new ArrayList<>();

        for (Long itemId : offeredItemIds) {
            ItemEntity item = itemRepository.findById(itemId)
                    .orElseThrow(() -> notFound("Offered item was not found."));
            if (item.getStatus() != ItemStatus.ACTIVE) {
                throw conflict("Offered item '" + item.getTitle() + "' is no longer active.");
            }
            offeredItems.add(item);
        }

        // Archive receiver item
        archiveItem(receiverItem);

        // Archive all offered items
        for (ItemEntity item : offeredItems) {
            archiveItem(item);
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Collect all item IDs involved in this trade for competing offer detection
        Set<Long> involvedItemIds = new HashSet<>();
        involvedItemIds.add(saved.getReceiverItemId());
        involvedItemIds.addAll(offeredItemIds);

        // Reject all competing PENDING offers involving any accepted item
        // (no notifications for auto-rejected competing offers — MVP scope)
        List<TradeOfferEntity> competing = tradeOfferRepository.findCompetingPendingOffers(
                saved.getId(), involvedItemIds);
        for (TradeOfferEntity comp : competing) {
            comp.reject();
            tradeOfferRepository.save(comp);
        }

        UserEntity sender = userRepository.findById(saved.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));

        // Notify the sender that their offer was accepted
        notificationService.createNotification(
                sender.getId(),
                NotificationType.TRADE_OFFER_ACCEPTED,
                user.getUsername() + " accepted your trade offer",
                user.getUsername() + " accepted your offer for \"" + receiverItem.getTitle() + "\".",
                saved.getUuid(),
                "TRADE_OFFER");

        return toResponse(saved, sender, user, offeredItems, receiverItem, user.getId());
    }

    @Override
    public TradeOfferResponse confirmCompletion(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        if (!isParticipant(offer, user)) {
            throw forbidden("You are not a participant of this trade offer.");
        }

        boolean actorIsSender = offer.getSenderUserId().equals(user.getId());

        try {
            if (actorIsSender) {
                offer.confirmCompletionBySender();
            } else {
                offer.confirmCompletionByReceiver();
            }
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        UserEntity sender = userRepository.findById(saved.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        UserEntity receiver = userRepository.findById(saved.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        ItemEntity receiverItem = itemRepository.findById(saved.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));
        List<ItemEntity> offeredItems = resolveOfferedItems(saved);

        if (saved.isCompleted()) {
            notifyTradeCompleted(saved, sender, receiver, receiverItem);
        } else {
            UserEntity counterparty = actorIsSender ? receiver : sender;
            notificationService.createNotification(
                    counterparty.getId(),
                    NotificationType.TRADE_OFFER_COMPLETION_CONFIRMED,
                    user.getUsername() + " confirmed trade completion",
                    user.getUsername() + " confirmed completion for the trade involving \""
                            + receiverItem.getTitle() + "\". Confirm once your side of the exchange is complete.",
                    saved.getUuid(),
                    "TRADE_OFFER");
        }

        return toResponse(saved, sender, receiver, offeredItems, receiverItem, user.getId());
    }

    // ── Reject ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse rejectOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only receiver can reject
        if (!offer.getReceiverUserId().equals(user.getId())) {
            throw forbidden("Only the receiver can reject a trade offer.");
        }

        try {
            offer.reject();
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Notify the sender that their offer was rejected
        UserEntity sender = userRepository.findById(saved.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        notificationService.createNotification(
                sender.getId(),
                NotificationType.TRADE_OFFER_REJECTED,
                user.getUsername() + " rejected your trade offer",
                user.getUsername() + " rejected your trade offer.",
                saved.getUuid(),
                "TRADE_OFFER");

        return toFullResponse(saved, user.getId());
    }

    // ── Cancel ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse cancelOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only sender can cancel
        if (!offer.getSenderUserId().equals(user.getId())) {
            throw forbidden("Only the sender can cancel a trade offer.");
        }

        try {
            offer.cancel();
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Notify the receiver that the offer was cancelled
        UserEntity receiver = userRepository.findById(saved.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        notificationService.createNotification(
                receiver.getId(),
                NotificationType.TRADE_OFFER_CANCELLED,
                user.getUsername() + " cancelled their trade offer",
                user.getUsername() + " cancelled their trade offer.",
                saved.getUuid(),
                "TRADE_OFFER");

        return toFullResponse(saved, user.getId());
    }

    // ── Private helpers ──────────────────────────────────────────

    private void validateModeConstraints(TradeOfferMode mode, List<UUID> senderItemUuids, String message) {
        boolean hasSenderItems = senderItemUuids != null && !senderItemUuids.isEmpty();

        switch (mode) {
            case ITEM_EXCHANGE -> {
                if (!hasSenderItems) {
                    throw badRequest("ITEM_EXCHANGE mode requires at least one sender item.");
                }
            }
            case GIFT -> {
                if (hasSenderItems) {
                    throw badRequest("GIFT mode does not allow sender items.");
                }
                if (message == null || message.isBlank()) {
                    throw badRequest("Message is required for GIFT mode.");
                }
            }
            case NEGOTIABLE -> {
                if (message == null || message.isBlank()) {
                    throw badRequest("Message is required for NEGOTIABLE mode.");
                }
            }
        }

        // Check for duplicates
        if (hasSenderItems) {
            Set<UUID> unique = new HashSet<>(senderItemUuids);
            if (unique.size() != senderItemUuids.size()) {
                throw badRequest("Duplicate sender item UUIDs are not allowed.");
            }
        }
    }

    private List<ItemEntity> resolveSenderItems(List<UUID> senderItemUuids, UserEntity sender) {
        if (senderItemUuids == null || senderItemUuids.isEmpty()) {
            return List.of();
        }

        List<ItemEntity> items = new ArrayList<>();
        for (UUID uuid : senderItemUuids) {
            ItemEntity item = resolveItem(uuid);
            if (!item.getOwnerId().equals(sender.getId())) {
                throw forbidden("Sender item '%s' does not belong to the authenticated user.".formatted(uuid));
            }
            if (item.getStatus() != ItemStatus.ACTIVE) {
                throw conflict("Sender item '%s' is not active.".formatted(uuid));
            }
            items.add(item);
        }
        return items;
    }

    private List<ItemListingEntryEntity> validateRequestedEntries(ItemEntity receiverItem, List<UUID> requestedEntryUuids) {
        List<UUID> uuids = requestedEntryUuids == null ? List.of() : requestedEntryUuids;
        boolean hasRequestedEntries = !uuids.isEmpty();
        ListingMode listingMode = receiverItem.getListingMode() == null ? ListingMode.SINGLE : receiverItem.getListingMode();

        if (listingMode != ListingMode.PICK_ANY) {
            if (hasRequestedEntries) {
                throw badRequest("Requested child entries are supported only for PICK_ANY listings.");
            }
            return List.of();
        }

        if (!hasRequestedEntries) {
            throw badRequest("At least one requested child entry must be selected for PICK_ANY listings.");
        }

        Set<UUID> unique = new HashSet<>(uuids);
        if (unique.size() != uuids.size()) {
            throw badRequest("Duplicate requested entry UUIDs are not allowed.");
        }

        List<ItemListingEntryEntity> entries = itemListingEntryRepository
                .findByItemIdAndUuidInOrderBySortOrderAsc(receiverItem.getId(), uuids);
        if (entries.size() != uuids.size()) {
            throw badRequest("Requested child entries must belong to the target listing.");
        }
        return entries;
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private ItemEntity resolveItem(UUID itemUuid) {
        return itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));
    }

    private TradeOfferEntity resolveOffer(UUID offerUuid) {
        return tradeOfferRepository.findByUuid(offerUuid)
                .orElseThrow(() -> notFound("Trade offer with uuid '%s' was not found.", offerUuid));
    }

    private boolean isParticipant(TradeOfferEntity offer, UserEntity user) {
        return offer.getSenderUserId().equals(user.getId())
                || offer.getReceiverUserId().equals(user.getId());
    }

    private void archiveItem(ItemEntity item) {
        item.setStatus(ItemStatus.ARCHIVED);
        item.setArchivedAt(OffsetDateTime.now());
        itemRepository.save(item);
    }

    private List<ItemEntity> resolveOfferedItems(TradeOfferEntity offer) {
        List<Long> offeredItemIds = tradeOfferItemRepository.findItemIdsByTradeOfferIdAndSide(
                offer.getId(), TradeOfferItemSide.OFFERED);
        return offeredItemIds.stream()
                .map(id -> itemRepository.findById(id)
                        .orElseThrow(() -> notFound("Offered item was not found.")))
                .toList();
    }

    private CategoryEntity resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("Category was not found."));
    }

    /**
     * Build a full TradeOfferResponse with all embedded summaries.
     */
    private TradeOfferResponse toFullResponse(TradeOfferEntity offer, Long currentUserId) {
        UserEntity sender = userRepository.findById(offer.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        UserEntity receiver = userRepository.findById(offer.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));

        List<ItemEntity> offeredItems = resolveOfferedItems(offer);

        return toResponse(offer, sender, receiver, offeredItems, receiverItem, currentUserId);
    }

    private TradeOfferResponse toResponse(TradeOfferEntity offer,
                                           UserEntity sender,
                                           UserEntity receiver,
                                           List<ItemEntity> offeredItems,
                                           ItemEntity receiverItem,
                                           Long currentUserId) {
        CategoryEntity receiverCategory = resolveCategory(receiverItem.getCategoryId());
        List<CategoryEntity> offeredCategories = offeredItems.stream()
                .map(item -> resolveCategory(item.getCategoryId()))
                .toList();

        TradeOfferResponse response = tradeOfferMapper.toResponse(offer, sender, receiver,
                receiverItem, receiverCategory, offeredItems, offeredCategories);
        response.setRequestedEntries(loadRequestedEntryResponses(offer));
        enrichCompletionState(response, offer, currentUserId);
        enrichReviewState(response, offer, sender, receiver, currentUserId);
        return response;
    }

    /**
     * Build a TradeOfferSummaryResponse for list endpoints.
     */
    private TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity offer, Long currentUserId) {
        UserEntity sender = userRepository.findById(offer.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        UserEntity receiver = userRepository.findById(offer.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));
        CategoryEntity receiverCategory = resolveCategory(receiverItem.getCategoryId());

        List<ItemEntity> offeredItems = resolveOfferedItems(offer);
        List<CategoryEntity> offeredCategories = offeredItems.stream()
                .map(item -> resolveCategory(item.getCategoryId()))
                .toList();

        TradeOfferSummaryResponse response = tradeOfferMapper.toSummaryResponse(offer, sender, receiver,
                receiverItem, receiverCategory, offeredItems, offeredCategories);
        response.setRequestedEntries(loadRequestedEntryResponses(offer));
        enrichCompletionState(response, offer, currentUserId);
        enrichReviewState(response, offer, currentUserId);
        return response;
    }

    private List<ItemListingEntryResponse> loadRequestedEntryResponses(TradeOfferEntity offer) {
        List<Long> requestedEntryIds = tradeOfferRequestedEntryRepository.findEntryIdsByTradeOfferId(offer.getId());
        if (requestedEntryIds.isEmpty()) {
            return List.of();
        }
        List<ItemListingEntryEntity> entries = itemListingEntryRepository.findByIdInOrderBySortOrderAsc(requestedEntryIds);
        return entries.stream()
                .map(this::toRequestedEntryResponse)
                .toList();
    }

    private ItemListingEntryResponse toRequestedEntryResponse(ItemListingEntryEntity entry) {
        return new ItemListingEntryResponse()
                .uuid(entry.getUuid())
                .title(entry.getTitle())
                .description(entry.getDescription())
                .quantity(entry.getQuantity())
                .sortOrder(entry.getSortOrder());
    }

    private void enrichCompletionState(TradeOfferResponse response, TradeOfferEntity offer, Long currentUserId) {
        boolean participant = offer.getSenderUserId().equals(currentUserId) || offer.getReceiverUserId().equals(currentUserId);
        boolean confirmed = hasCurrentUserConfirmedCompletion(offer, currentUserId);
        response.setCurrentUserCompletionConfirmed(participant && confirmed);
        response.setCanConfirmCompletion(participant && offer.isAccepted() && !confirmed);
    }

    private void enrichCompletionState(TradeOfferSummaryResponse response, TradeOfferEntity offer, Long currentUserId) {
        boolean participant = offer.getSenderUserId().equals(currentUserId) || offer.getReceiverUserId().equals(currentUserId);
        boolean confirmed = hasCurrentUserConfirmedCompletion(offer, currentUserId);
        response.setCurrentUserCompletionConfirmed(participant && confirmed);
        response.setCanConfirmCompletion(participant && offer.isAccepted() && !confirmed);
    }

    private void enrichReviewState(
            TradeOfferResponse response,
            TradeOfferEntity offer,
            UserEntity sender,
            UserEntity receiver,
            Long currentUserId) {
        ReviewState reviewState = buildReviewState(offer, currentUserId);
        response.setCanCurrentUserReview(reviewState.canCurrentUserReview());
        response.setCurrentUserHasReviewed(reviewState.currentUserReview() != null);
        response.setCounterpartyHasReviewed(reviewState.counterpartyReview() != null);
        response.setCurrentUserReview(reviewState.currentUserReview() == null
                ? null
                : tradeReviewMapper.toEmbeddedResponse(reviewState.currentUserReview(), offer,
                        reviewState.currentUserReviewer().equals(sender.getId()) ? sender : receiver,
                        reviewState.currentUserReviewed().equals(sender.getId()) ? sender : receiver));
        response.setCounterpartyReview(reviewState.counterpartyReview() == null
                ? null
                : tradeReviewMapper.toEmbeddedResponse(reviewState.counterpartyReview(), offer,
                        reviewState.counterpartyReviewer().equals(sender.getId()) ? sender : receiver,
                        reviewState.counterpartyReviewed().equals(sender.getId()) ? sender : receiver));
    }

    private void enrichReviewState(TradeOfferSummaryResponse response, TradeOfferEntity offer, Long currentUserId) {
        ReviewState reviewState = buildReviewState(offer, currentUserId);
        response.setCanCurrentUserReview(reviewState.canCurrentUserReview());
        response.setCurrentUserHasReviewed(reviewState.currentUserReview() != null);
        response.setCounterpartyHasReviewed(reviewState.counterpartyReview() != null);
    }

    private ReviewState buildReviewState(TradeOfferEntity offer, Long currentUserId) {
        boolean participant = offer.getSenderUserId().equals(currentUserId) || offer.getReceiverUserId().equals(currentUserId);
        if (!participant) {
            return new ReviewState(false, null, null, null, null, null, null);
        }

        List<TradeReviewEntity> reviews = tradeReviewRepository.findByTradeOfferId(offer.getId());
        TradeReviewEntity currentUserReview = reviews.stream()
                .filter(review -> review.getReviewerUserId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        TradeReviewEntity counterpartyReview = reviews.stream()
                .filter(review -> !review.getReviewerUserId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        boolean canCurrentUserReview = offer.getStatus() == TradeOfferStatus.COMPLETED && currentUserReview == null;

        return new ReviewState(
                canCurrentUserReview,
                currentUserReview,
                counterpartyReview,
                currentUserReview != null ? currentUserReview.getReviewerUserId() : null,
                currentUserReview != null ? currentUserReview.getReviewedUserId() : null,
                counterpartyReview != null ? counterpartyReview.getReviewerUserId() : null,
                counterpartyReview != null ? counterpartyReview.getReviewedUserId() : null);
    }

    private boolean hasCurrentUserConfirmedCompletion(TradeOfferEntity offer, Long currentUserId) {
        if (offer.getSenderUserId().equals(currentUserId)) {
            return offer.getSenderCompletedAt() != null;
        }
        if (offer.getReceiverUserId().equals(currentUserId)) {
            return offer.getReceiverCompletedAt() != null;
        }
        return false;
    }

    private record ReviewState(
            boolean canCurrentUserReview,
            TradeReviewEntity currentUserReview,
            TradeReviewEntity counterpartyReview,
            Long currentUserReviewer,
            Long currentUserReviewed,
            Long counterpartyReviewer,
            Long counterpartyReviewed) {
    }

    private void notifyTradeCompleted(
            TradeOfferEntity offer,
            UserEntity sender,
            UserEntity receiver,
            ItemEntity receiverItem) {
        notificationService.createNotification(
                sender.getId(),
                NotificationType.TRADE_OFFER_COMPLETED,
                "Trade completed with " + receiver.getUsername(),
                "Your trade with " + receiver.getUsername() + " for \"" + receiverItem.getTitle()
                        + "\" has been marked completed.",
                offer.getUuid(),
                "TRADE_OFFER");

        notificationService.createNotification(
                receiver.getId(),
                NotificationType.TRADE_OFFER_COMPLETED,
                "Trade completed with " + sender.getUsername(),
                "Your trade with " + sender.getUsername() + " for \"" + receiverItem.getTitle()
                        + "\" has been marked completed.",
                offer.getUuid(),
                "TRADE_OFFER");
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }

    private ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                message);
    }

    private ApiException conflict(String message) {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                message);
    }

    private ApiException badRequest(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                message);
    }
}

